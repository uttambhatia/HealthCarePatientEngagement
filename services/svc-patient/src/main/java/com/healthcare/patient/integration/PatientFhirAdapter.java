package com.healthcare.patient.integration;

import com.healthcare.patient.domain.PatientProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class PatientFhirAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PatientFhirAdapter.class);
    private final RestClient restClient;
    private final String path;
    private final int maxAttempts;

    public PatientFhirAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${platform.integration.fhir.base-url:}") String baseUrl,
            @Value("${platform.integration.fhir.path:/fhir/patients}") String path,
            @Value("${platform.messaging.retryAttempts:3}") int maxAttempts) {
        this.restClient = baseUrl.isBlank() ? null : restClientBuilder.baseUrl(baseUrl).build();
        this.path = path;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    public void synchronizeProfile(PatientProfile aggregate, String correlationId) {
        if (restClient == null) {
            LOGGER.warn("Skipping patient FHIR integration for aggregateId={} because base URL is not configured", aggregate.id());
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", aggregate.id());
        payload.put("status", aggregate.status());
        payload.put("externalReference", aggregate.externalReference());
        payload.put("givenName", aggregate.givenName());
        payload.put("familyName", aggregate.familyName());
        payload.put("birthDate", aggregate.birthDate());
        payload.put("email", aggregate.email());
        payload.put("phone", aggregate.phone());
        payload.put("demographics", aggregate.demographics());

        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                restClient.post()
                        .uri(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-Id", correlationId)
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity();
                LOGGER.info("Patient FHIR integration succeeded aggregateId={} correlationId={} attempt={}", aggregate.id(), correlationId, attempt);
                return;
            } catch (RuntimeException ex) {
                lastError = ex;
                LOGGER.warn("Patient FHIR integration attempt failed aggregateId={} correlationId={} attempt={} maxAttempts={} error={}",
                        aggregate.id(), correlationId, attempt, maxAttempts, ex.getMessage());
            }
        }

        throw new IllegalStateException("Patient FHIR integration failed after retries for aggregateId=" + aggregate.id(), lastError);
    }
}
