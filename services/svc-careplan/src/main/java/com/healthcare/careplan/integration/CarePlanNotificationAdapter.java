package com.healthcare.careplan.integration;

import com.healthcare.careplan.domain.CarePlanAggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CarePlanNotificationAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarePlanNotificationAdapter.class);

    private final RestClient restClient;
    private final String path;
    private final int maxAttempts;

    public CarePlanNotificationAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${platform.integration.notification.base-url:}") String baseUrl,
            @Value("${platform.integration.notification.path:/notifications/careplans}") String path,
            @Value("${platform.messaging.retryAttempts:3}") int maxAttempts) {
        this.restClient = baseUrl.isBlank() ? null : restClientBuilder.baseUrl(baseUrl).build();
        this.path = path;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    public void sendCarePlanNotification(CarePlanAggregate aggregate, String action, String correlationId) {
        if (restClient == null) {
            LOGGER.warn("Skipping careplan notification integration for aggregateId={} because base URL is not configured", aggregate.id());
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", aggregate.id());
        payload.put("status", aggregate.status());
        payload.put("patientId", aggregate.patientId());
        payload.put("goal", aggregate.goal());
        payload.put("planStatus", aggregate.planStatus());
        payload.put("ownerId", aggregate.ownerId());
        payload.put("tasks", aggregate.tasks());
        payload.put("version", aggregate.version());
        payload.put("action", action);

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
                LOGGER.info("Careplan notification integration succeeded aggregateId={} correlationId={} attempt={}", aggregate.id(), correlationId, attempt);
                return;
            } catch (RuntimeException ex) {
                lastError = ex;
                LOGGER.warn("Careplan notification integration attempt failed aggregateId={} correlationId={} attempt={} maxAttempts={} error={}",
                        aggregate.id(), correlationId, attempt, maxAttempts, ex.getMessage());
            }
        }

        throw new IllegalStateException("Careplan notification integration failed after retries for aggregateId=" + aggregate.id(), lastError);
    }
}
