package com.healthcare.medicalrecord.integration;

import com.healthcare.medicalrecord.domain.MedicalRecordSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class FhirAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(FhirAdapter.class);
    private final RestClient restClient;
    private final String path;
    private final int maxAttempts;

    public FhirAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${platform.integration.fhir.base-url:}") String baseUrl,
            @Value("${platform.integration.fhir.path:/fhir/medical-records}") String path,
            @Value("${platform.messaging.retryAttempts:3}") int maxAttempts) {
        this.restClient = baseUrl.isBlank() ? null : restClientBuilder.baseUrl(baseUrl).build();
        this.path = path;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    public void upsertFhirResource(MedicalRecordSnapshot aggregate, String correlationId) {
        if (restClient == null) {
            LOGGER.warn("Skipping medical record FHIR integration for aggregateId={} because base URL is not configured", aggregate.id());
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", aggregate.id());
        payload.put("status", aggregate.status());
        payload.put("patientId", aggregate.patientId());
        payload.put("fhirResourceType", aggregate.fhirResourceType());
        payload.put("resourceReference", aggregate.resourceReference());
        payload.put("summary", aggregate.summary());
        payload.put("version", aggregate.version());

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
                LOGGER.info("Medical record FHIR integration succeeded aggregateId={} correlationId={} attempt={}", aggregate.id(), correlationId, attempt);
                return;
            } catch (RuntimeException ex) {
                lastError = ex;
                LOGGER.warn("Medical record FHIR integration attempt failed aggregateId={} correlationId={} attempt={} maxAttempts={} error={}",
                        aggregate.id(), correlationId, attempt, maxAttempts, ex.getMessage());
            }
        }

        throw new IllegalStateException("Medical record FHIR integration failed after retries for aggregateId=" + aggregate.id(), lastError);
    }
}
