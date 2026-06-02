package com.healthcare.appointment.integration;

import com.healthcare.appointment.domain.AppointmentRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AppointmentNotificationAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppointmentNotificationAdapter.class);
    private final RestClient restClient;
    private final String path;
    private final int maxAttempts;

    public AppointmentNotificationAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${platform.integration.notification.base-url:}") String baseUrl,
            @Value("${platform.integration.notification.path:/notifications/appointments}") String path,
            @Value("${platform.messaging.retryAttempts:3}") int maxAttempts) {
        this.restClient = baseUrl.isBlank() ? null : restClientBuilder.baseUrl(baseUrl).build();
        this.path = path;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    public void sendBookingNotification(AppointmentRecord aggregate, String correlationId) {
        if (restClient == null) {
            LOGGER.warn("Skipping appointment notification integration for aggregateId={} because base URL is not configured", aggregate.id());
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", aggregate.id());
        payload.put("status", aggregate.status());
        payload.put("patientId", aggregate.patientId());
        payload.put("providerId", aggregate.providerId());
        payload.put("scheduledAt", aggregate.scheduledAt());
        payload.put("channel", aggregate.channel());

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
                LOGGER.info("Appointment notification integration succeeded aggregateId={} correlationId={} attempt={}", aggregate.id(), correlationId, attempt);
                return;
            } catch (RuntimeException ex) {
                lastError = ex;
                LOGGER.warn("Appointment notification integration attempt failed aggregateId={} correlationId={} attempt={} maxAttempts={} error={}",
                        aggregate.id(), correlationId, attempt, maxAttempts, ex.getMessage());
            }
        }

        throw new IllegalStateException("Appointment notification integration failed after retries for aggregateId=" + aggregate.id(), lastError);
    }
}
