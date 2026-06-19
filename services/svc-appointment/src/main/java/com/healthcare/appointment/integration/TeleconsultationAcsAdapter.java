package com.healthcare.appointment.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class TeleconsultationAcsAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeleconsultationAcsAdapter.class);

    private final RestClient restClient;
    private final String path;
    private final int maxAttempts;

    public TeleconsultationAcsAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${platform.integration.teleconsult.acs-base-url:}") String baseUrl,
            @Value("${platform.integration.teleconsult.acs-path:/acs/teleconsult/sessions}") String path,
            @Value("${platform.messaging.retryAttempts:3}") int maxAttempts) {
        this.restClient = baseUrl.isBlank() ? null : restClientBuilder.baseUrl(baseUrl).build();
        this.path = path;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    public Optional<TeleconsultJoinUrls> createSession(
            String appointmentId,
            String patientId,
            String providerId,
            String correlationId) {
        if (restClient == null) {
            LOGGER.info("Skipping ACS teleconsult session provisioning because base URL is not configured appointmentId={} correlationId={}",
                    appointmentId, correlationId);
            return Optional.empty();
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("appointmentId", appointmentId);
        payload.put("patientId", patientId);
        payload.put("providerId", providerId);

        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = restClient.post()
                        .uri(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-Id", correlationId)
                        .body(payload)
                        .retrieve()
                        .body(Map.class);

                if (body == null) {
                    throw new IllegalStateException("ACS teleconsult integration returned empty body");
                }

                String doctorJoinUrl = text(body, "doctorJoinUrl");
                String patientJoinUrl = text(body, "patientJoinUrl");

                if (doctorJoinUrl.isBlank() || patientJoinUrl.isBlank()) {
                    throw new IllegalStateException("ACS teleconsult integration response is missing join URLs");
                }

                LOGGER.info("ACS teleconsult session provisioning succeeded appointmentId={} correlationId={} attempt={}",
                        appointmentId, correlationId, attempt);
                return Optional.of(new TeleconsultJoinUrls(doctorJoinUrl, patientJoinUrl));
            } catch (RuntimeException ex) {
                lastError = ex;
                LOGGER.warn("ACS teleconsult session provisioning attempt failed appointmentId={} correlationId={} attempt={} maxAttempts={} error={}",
                        appointmentId, correlationId, attempt, maxAttempts, ex.getMessage());
            }
        }

        LOGGER.error("ACS teleconsult session provisioning failed after retries appointmentId={} correlationId={}",
                appointmentId, correlationId, lastError);
        return Optional.empty();
    }

    private String text(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    public record TeleconsultJoinUrls(String doctorJoinUrl, String patientJoinUrl) {
    }
}
