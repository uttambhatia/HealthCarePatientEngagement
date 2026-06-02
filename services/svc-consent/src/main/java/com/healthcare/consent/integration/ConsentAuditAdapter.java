package com.healthcare.consent.integration;

import com.healthcare.consent.domain.ConsentRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ConsentAuditAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsentAuditAdapter.class);
    private final RestClient restClient;
    private final String path;
    private final int maxAttempts;

    public ConsentAuditAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${platform.integration.audit.base-url:}") String baseUrl,
            @Value("${platform.integration.audit.path:/audit/consents}") String path,
            @Value("${platform.messaging.retryAttempts:3}") int maxAttempts) {
        this.restClient = baseUrl.isBlank() ? null : restClientBuilder.baseUrl(baseUrl).build();
        this.path = path;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    public void publishAuditTrail(ConsentRecord aggregate, String correlationId) {
        if (restClient == null) {
            LOGGER.warn("Skipping consent audit integration for aggregateId={} because base URL is not configured", aggregate.id());
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", aggregate.id());
        payload.put("status", aggregate.status());
        payload.put("patientId", aggregate.patientId());
        payload.put("consentType", aggregate.consentType());
        payload.put("granted", aggregate.granted());
        payload.put("version", aggregate.version());
        payload.put("effectiveFrom", aggregate.effectiveFrom());

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
                LOGGER.info("Consent audit integration succeeded aggregateId={} correlationId={} attempt={}", aggregate.id(), correlationId, attempt);
                return;
            } catch (RuntimeException ex) {
                lastError = ex;
                LOGGER.warn("Consent audit integration attempt failed aggregateId={} correlationId={} attempt={} maxAttempts={} error={}",
                        aggregate.id(), correlationId, attempt, maxAttempts, ex.getMessage());
            }
        }

        throw new IllegalStateException("Consent audit integration failed after retries for aggregateId=" + aggregate.id(), lastError);
    }
}
