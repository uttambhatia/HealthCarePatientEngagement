package com.healthcare.notification.integration;

import com.healthcare.notification.port.AppointmentBookingPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class FollowUpAppointmentAdapter implements AppointmentBookingPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(FollowUpAppointmentAdapter.class);

    private final RestClient restClient;
    private final String path;
    private final int maxAttempts;
    private final String defaultChannel;

    public FollowUpAppointmentAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${platform.integration.appointment.base-url:}") String baseUrl,
            @Value("${platform.integration.appointment.path:/appointments}") String path,
            @Value("${platform.messaging.retryAttempts:3}") int maxAttempts,
            @Value("${platform.teleconsult.followup.appointment.channel:VIDEO}") String defaultChannel) {
        this.restClient = baseUrl.isBlank() ? null : restClientBuilder.baseUrl(baseUrl).build();
        this.path = path;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.defaultChannel = defaultChannel;
    }

    /**
     * Posts a follow-up appointment draft to the appointment service.
     * If the base URL is not configured the call is skipped with a warning — this is intentional so
     * notification service starts cleanly in environments where appointment service is not yet reachable.
     *
     * @return true when the appointment was created, false when skipped (no base-url), throws on delivery failure.
     */
    public boolean createFollowUpAppointment(
            String patientId,
            String providerId,
            String scheduledAt,
            String correlationId) {

        if (restClient == null) {
            LOGGER.warn("Skipping follow-up appointment creation because appointment base URL is not configured patientId={} correlationId={}",
                    patientId, correlationId);
            return false;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("patientId", patientId);
        payload.put("providerId", providerId);
        payload.put("scheduledAt", scheduledAt);
        payload.put("channel", defaultChannel);

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
                LOGGER.info("Follow-up appointment created patientId={} correlationId={} attempt={}",
                        patientId, correlationId, attempt);
                return true;
            } catch (RuntimeException ex) {
                lastError = ex;
                LOGGER.warn("Follow-up appointment creation attempt failed patientId={} correlationId={} attempt={} maxAttempts={} error={}",
                        patientId, correlationId, attempt, maxAttempts, ex.getMessage());
            }
        }

        throw new IllegalStateException(
                "Follow-up appointment creation failed after retries for patientId=" + patientId, lastError);
    }
}
