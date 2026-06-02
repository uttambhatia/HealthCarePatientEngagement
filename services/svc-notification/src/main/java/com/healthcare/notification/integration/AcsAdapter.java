package com.healthcare.notification.integration;

import com.healthcare.notification.domain.NotificationJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AcsAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcsAdapter.class);
    private final RestClient restClient;
    private final String path;
    private final int maxAttempts;

    public AcsAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${platform.integration.acs.base-url:}") String baseUrl,
            @Value("${platform.integration.acs.path:/acs/notifications}") String path,
            @Value("${platform.messaging.retryAttempts:3}") int maxAttempts) {
        this.restClient = baseUrl.isBlank() ? null : restClientBuilder.baseUrl(baseUrl).build();
        this.path = path;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    public int dispatchNotification(NotificationJob aggregate, String correlationId) {
        if (restClient == null) {
            LOGGER.warn("Skipping ACS integration for aggregateId={} because base URL is not configured", aggregate.id());
            return 0;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", aggregate.id());
        payload.put("status", aggregate.status());
        payload.put("recipientId", aggregate.recipientId());
        payload.put("channel", aggregate.channel());
        payload.put("templateId", aggregate.templateId());
        payload.put("message", aggregate.message());

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
                LOGGER.info("ACS integration succeeded aggregateId={} correlationId={} attempt={}", aggregate.id(), correlationId, attempt);
                return attempt;
            } catch (RuntimeException ex) {
                lastError = ex;
                LOGGER.warn("ACS integration attempt failed aggregateId={} correlationId={} attempt={} maxAttempts={} error={}",
                        aggregate.id(), correlationId, attempt, maxAttempts, ex.getMessage());
            }
        }

        throw new IllegalStateException("ACS integration failed after retries for aggregateId=" + aggregate.id(), lastError);
    }

    public int maxAttempts() {
        return maxAttempts;
    }
}
