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
public class IoTAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(IoTAdapter.class);
    private final RestClient restClient;
    private final String path;
    private final int maxAttempts;

    public IoTAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${platform.integration.iot.base-url:}") String baseUrl,
            @Value("${platform.integration.iot.path:/iot/messages}") String path,
            @Value("${platform.messaging.retryAttempts:3}") int maxAttempts) {
        this.restClient = baseUrl.isBlank() ? null : restClientBuilder.baseUrl(baseUrl).build();
        this.path = path;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    public void ingest(DeviceMessage aggregate, String correlationId) {
        if (restClient == null) {
            LOGGER.warn("Skipping IoT integration for aggregateId={} because base URL is not configured", aggregate.id());
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", aggregate.id());
        payload.put("status", aggregate.status());
        payload.put("deviceId", aggregate.deviceId());
        payload.put("protocol", aggregate.protocol());
        payload.put("payload", aggregate.payload());
        payload.put("receivedAt", aggregate.receivedAt());

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
                LOGGER.info("IoT integration succeeded aggregateId={} correlationId={} attempt={}", aggregate.id(), correlationId, attempt);
                return;
            } catch (RuntimeException ex) {
                lastError = ex;
                LOGGER.warn("IoT integration attempt failed aggregateId={} correlationId={} attempt={} maxAttempts={} error={}",
                        aggregate.id(), correlationId, attempt, maxAttempts, ex.getMessage());
            }
        }

        throw new IllegalStateException("IoT integration failed after retries for aggregateId=" + aggregate.id(), lastError);
    }
}
