package com.healthcare.alertmanagement.integration;

import com.healthcare.alertmanagement.domain.ClinicalAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AlertEscalationAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlertEscalationAdapter.class);
    private final RestClient restClient;
    private final String path;
    private final int maxAttempts;

    public AlertEscalationAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${platform.integration.alert-escalation.base-url:}") String baseUrl,
            @Value("${platform.integration.alert-escalation.path:/alerts/escalations}") String path,
            @Value("${platform.messaging.retryAttempts:3}") int maxAttempts) {
        this.restClient = baseUrl.isBlank() ? null : restClientBuilder.baseUrl(baseUrl).build();
        this.path = path;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    public void escalateAlert(ClinicalAlert aggregate, String correlationId) {
        if (restClient == null) {
            LOGGER.warn("Skipping alert escalation integration for aggregateId={} because base URL is not configured", aggregate.id());
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", aggregate.id());
        payload.put("status", aggregate.status());
        payload.put("patientId", aggregate.patientId());
        payload.put("deviceId", aggregate.deviceId());
        payload.put("severity", aggregate.severity());
        payload.put("triggerType", aggregate.triggerType());
        payload.put("summary", aggregate.summary());

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
                LOGGER.info("Alert escalation integration succeeded aggregateId={} correlationId={} attempt={}", aggregate.id(), correlationId, attempt);
                return;
            } catch (RuntimeException ex) {
                lastError = ex;
                LOGGER.warn("Alert escalation integration attempt failed aggregateId={} correlationId={} attempt={} maxAttempts={} error={}",
                        aggregate.id(), correlationId, attempt, maxAttempts, ex.getMessage());
            }
        }

        throw new IllegalStateException("Alert escalation integration failed after retries for aggregateId=" + aggregate.id(), lastError);
    }
}
