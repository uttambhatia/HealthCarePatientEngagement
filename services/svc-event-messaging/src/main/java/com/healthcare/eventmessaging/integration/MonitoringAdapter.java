package com.healthcare.eventmessaging.integration;

import com.healthcare.eventmessaging.domain.ServiceBusMessageRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MonitoringAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringAdapter.class);

    private final RestClient restClient;
    private final String path;
    private final int maxAttempts;

    public MonitoringAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${platform.integration.monitoring.base-url:}") String baseUrl,
            @Value("${platform.integration.monitoring.path:/monitoring/logs}") String path,
            @Value("${platform.messaging.retryAttempts:3}") int maxAttempts) {
        this.restClient = baseUrl.isBlank() ? null : restClientBuilder.baseUrl(baseUrl).build();
        this.path = path;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    public void publishAuditRecord(ServiceBusMessageRecord aggregate, String correlationId) {
        if (restClient == null) {
            LOGGER.warn("Skipping monitoring integration for aggregateId={} because base URL is not configured", aggregate.id());
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", aggregate.id());
        payload.put("status", aggregate.status());
        payload.put("channel", aggregate.channel());
        payload.put("eventName", aggregate.eventName());
        payload.put("messageType", aggregate.messageType());
        payload.put("recordedAt", aggregate.recordedAt());
        payload.put("integrityHash", aggregate.integrityHash());
        payload.put("anomalyReason", aggregate.anomalyReason());

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
                LOGGER.info("Monitoring integration succeeded aggregateId={} correlationId={} attempt={}", aggregate.id(), correlationId, attempt);
                return;
            } catch (RuntimeException ex) {
                lastError = ex;
                LOGGER.warn("Monitoring integration attempt failed aggregateId={} correlationId={} attempt={} maxAttempts={} error={}",
                        aggregate.id(), correlationId, attempt, maxAttempts, ex.getMessage());
            }
        }

        throw new IllegalStateException("Monitoring integration failed after retries for aggregateId=" + aggregate.id(), lastError);
    }
}
