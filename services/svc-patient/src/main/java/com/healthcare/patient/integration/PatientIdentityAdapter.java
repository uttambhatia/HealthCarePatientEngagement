package com.healthcare.patient.integration;

import com.healthcare.patient.domain.PatientProfile;
import com.healthcare.patient.dto.CreatePatientRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class PatientIdentityAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PatientIdentityAdapter.class);

    private final RestClient restClient;
    private final String path;

    public PatientIdentityAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${platform.integration.identity-adapter.base-url:}") String baseUrl,
            @Value("${platform.integration.identity-adapter.path:/identity/assertions}") String path) {
        this.restClient = baseUrl.isBlank() ? null : restClientBuilder.baseUrl(baseUrl).build();
        this.path = path;
    }

    public void provisionIdentity(CreatePatientRequest request, String correlationId) {
        dispatch(request.externalReference(), correlationId);
    }

    public void provisionIdentity(PatientProfile profile, String correlationId) {
        dispatch(profile.externalReference(), correlationId);
    }

    private void dispatch(String subject, String correlationId) {
        if (restClient == null) {
            LOGGER.warn("Skipping identity provisioning for subject={} because base URL is not configured", subject);
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("subject", subject);
        payload.put("tenantId", "healthcare-tenant");
        payload.put("role", "PATIENT");
        payload.put("tokenId", correlationId);

        restClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Correlation-Id", correlationId)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }
}
