package com.healthcare.deviceingestion.integration;

import com.healthcare.deviceingestion.domain.DeviceMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class TelemetryIngestionAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryIngestionAdapter.class);

    private final RestClient restClient;
    private final String path;
    private final int maxAttempts;

    public TelemetryIngestionAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${platform.integration.telemetry.base-url:}") String baseUrl,
            @Value("${platform.integration.telemetry.path:/telemetry}") String path,
            @Value("${platform.messaging.retryAttempts:3}") int maxAttempts) {
        this.restClient = baseUrl.isBlank() ? null : restClientBuilder.baseUrl(baseUrl).build();
        this.path = path;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    public void forwardToTelemetry(DeviceMessage aggregate, String metricType, String metricValue, String correlationId) {
        if (restClient == null) {
            LOGGER.warn("Skipping telemetry forwarding for aggregateId={} because base URL is not configured", aggregate.id());
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("deviceId", aggregate.deviceId());
        payload.put("metricType", metricType);
        payload.put("metricValue", metricValue);
        payload.put("recordedAt", aggregate.receivedAt());

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
                LOGGER.info("Telemetry forwarding succeeded aggregateId={} correlationId={} attempt={}", aggregate.id(), correlationId, attempt);
                return;
            } catch (RuntimeException ex) {
                lastError = ex;
                LOGGER.warn("Telemetry forwarding attempt failed aggregateId={} correlationId={} attempt={} maxAttempts={} error={}",
                        aggregate.id(), correlationId, attempt, maxAttempts, ex.getMessage());
            }
        }

        throw new IllegalStateException("Telemetry forwarding failed after retries for aggregateId=" + aggregate.id(), lastError);
    }
}
