package com.healthcare.telemetry.integration;

import com.healthcare.telemetry.domain.TelemetryReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class TelemetryTimeSeriesAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryTimeSeriesAdapter.class);
    private final RestClient restClient;
    private final String path;
    private final int maxAttempts;

    public TelemetryTimeSeriesAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${platform.integration.time-series.base-url:}") String baseUrl,
            @Value("${platform.integration.time-series.path:/timeseries/telemetry}") String path,
            @Value("${platform.messaging.retryAttempts:3}") int maxAttempts) {
        this.restClient = baseUrl.isBlank() ? null : restClientBuilder.baseUrl(baseUrl).build();
        this.path = path;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    public void persistMetric(TelemetryReading aggregate, String correlationId) {
        if (restClient == null) {
            LOGGER.warn("Skipping telemetry time-series integration for aggregateId={} because base URL is not configured", aggregate.id());
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", aggregate.id());
        payload.put("status", aggregate.status());
        payload.put("deviceId", aggregate.deviceId());
        payload.put("metricType", aggregate.metricType());
        payload.put("metricValue", aggregate.metricValue());
        payload.put("recordedAt", aggregate.recordedAt());

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
                LOGGER.info("Telemetry time-series integration succeeded aggregateId={} correlationId={} attempt={}", aggregate.id(), correlationId, attempt);
                return;
            } catch (RuntimeException ex) {
                lastError = ex;
                LOGGER.warn("Telemetry time-series integration attempt failed aggregateId={} correlationId={} attempt={} maxAttempts={} error={}",
                        aggregate.id(), correlationId, attempt, maxAttempts, ex.getMessage());
            }
        }

        throw new IllegalStateException("Telemetry time-series integration failed after retries for aggregateId=" + aggregate.id(), lastError);
    }
}
