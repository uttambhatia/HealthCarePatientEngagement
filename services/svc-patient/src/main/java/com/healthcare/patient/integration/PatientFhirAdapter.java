package com.healthcare.patient.integration;

import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.patient.domain.PatientProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PatientFhirAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PatientFhirAdapter.class);
    private static final String FHIR_CONTENT_TYPE = "application/fhir+json";

    private final RestClient restClient;
    private final TokenCredential credential;
    private final String audience;
    private final int maxAttempts;
    private final ObjectMapper objectMapper;

    public PatientFhirAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${platform.integration.fhir.base-url:}") String baseUrl,
            @Value("${platform.integration.fhir.audience:}") String audience,
            @Value("${platform.messaging.retryAttempts:3}") int maxAttempts) {
        this.restClient = baseUrl.isBlank() ? null : restClientBuilder.baseUrl(baseUrl).build();
        this.credential = baseUrl.isBlank() ? null : new DefaultAzureCredentialBuilder().build();
        this.audience = audience;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.objectMapper = new ObjectMapper();
    }

    public void synchronizeProfile(PatientProfile aggregate, String correlationId) {
        if (restClient == null) {
            LOGGER.warn("Skipping patient FHIR integration for aggregateId={} because base URL is not configured", aggregate.id());
            return;
        }

        Map<String, Object> fhirPatient = buildFhirPatientResource(aggregate);
        String bearerToken = acquireToken();
        
        if (bearerToken == null || bearerToken.isBlank()) {
            LOGGER.warn("Skipping patient FHIR integration for aggregateId={} correlationId={} - no token available (check RBAC/permissions)", 
                    aggregate.id(), correlationId);
            return;
        }

        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                restClient.put()
                        .uri("/Patient/{id}", aggregate.id())
                        .contentType(MediaType.parseMediaType(FHIR_CONTENT_TYPE))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                        .header("X-Correlation-Id", correlationId)
                        .body(fhirPatient)
                        .retrieve()
                        .toBodilessEntity();
                LOGGER.info("Patient FHIR integration succeeded aggregateId={} correlationId={} attempt={}", aggregate.id(), correlationId, attempt);
                return;
            } catch (RuntimeException ex) {
                lastError = ex;
                String errorMsg = ex.getMessage();
                boolean isAuthError = errorMsg != null && (errorMsg.contains("401") || errorMsg.contains("403") || errorMsg.contains("Forbidden") || errorMsg.contains("Unauthorized"));
                
                if (isAuthError) {
                    LOGGER.warn("Patient FHIR integration skipped (authorization failed) aggregateId={} correlationId={} - managed identity lacks FHIR data-plane access. " +
                            "Grant 'FHIR Data Contributor' role to MI OID 0fe78f1a-9af1-47d8-a86b-604726f5101e at FHIR service scope. Error: {}",
                            aggregate.id(), correlationId, errorMsg);
                    return;
                } else {
                    LOGGER.warn("Patient FHIR integration attempt failed aggregateId={} correlationId={} attempt={} maxAttempts={} error={}",
                            aggregate.id(), correlationId, attempt, maxAttempts, errorMsg);
                }
            }
        }

        LOGGER.error("Patient FHIR integration failed after all retries for aggregateId={} correlationId={} - patient record created but FHIR sync incomplete", 
                aggregate.id(), correlationId);
    }

    private String acquireToken() {
        if (credential == null || audience == null || audience.isBlank()) {
            return null;
        }
        try {
            String scope = audience.endsWith("/.default") ? audience : audience + "/.default";
            return credential.getToken(new TokenRequestContext().addScopes(scope)).block().getToken();
        } catch (Exception ex) {
            LOGGER.warn("Failed to acquire token for FHIR access using managed identity: {}", ex.getMessage());
            return null;
        }
    }

    private Map<String, Object> buildFhirPatientResource(PatientProfile aggregate) {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("resourceType", "Patient");
        resource.put("id", aggregate.id());

        List<Map<String, Object>> identifiers = new ArrayList<>();
        Map<String, Object> identifier = new LinkedHashMap<>();
        identifier.put("system", "urn:healthcare:external-reference");
        identifier.put("value", aggregate.externalReference());
        identifiers.add(identifier);
        resource.put("identifier", identifiers);

        List<Map<String, Object>> names = new ArrayList<>();
        Map<String, Object> name = new LinkedHashMap<>();
        name.put("use", "official");
        name.put("family", aggregate.familyName());
        name.put("given", List.of(aggregate.givenName()));
        names.add(name);
        resource.put("name", names);

        if (aggregate.birthDate() != null && !aggregate.birthDate().isBlank()) {
            resource.put("birthDate", aggregate.birthDate());
        }

        List<Map<String, Object>> telecom = new ArrayList<>();
        if (aggregate.email() != null && !aggregate.email().isBlank()) {
            Map<String, Object> email = new LinkedHashMap<>();
            email.put("system", "email");
            email.put("value", aggregate.email());
            email.put("use", "work");
            telecom.add(email);
        }
        if (aggregate.phone() != null && !aggregate.phone().isBlank()) {
            Map<String, Object> phone = new LinkedHashMap<>();
            phone.put("system", "phone");
            phone.put("value", aggregate.phone());
            phone.put("use", "mobile");
            telecom.add(phone);
        }
        if (!telecom.isEmpty()) {
            resource.put("telecom", telecom);
        }

        String gender = extractGenderFromDemographics(aggregate.demographics());
        if (gender != null) {
            resource.put("gender", gender);
        }

        return resource;
    }

    private String extractGenderFromDemographics(String demographics) {
        if (demographics == null || demographics.isBlank()) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> demo = objectMapper.readValue(demographics, Map.class);
            Object raw = demo.get("gender");
            if (raw == null) {
                return null;
            }
            return switch (raw.toString().toUpperCase()) {
                case "M", "MALE" -> "male";
                case "F", "FEMALE" -> "female";
                case "OTHER" -> "other";
                default -> "unknown";
            };
        } catch (Exception e) {
            LOGGER.debug("Could not parse gender from demographics: {}", e.getMessage());
            return null;
        }
    }
}
