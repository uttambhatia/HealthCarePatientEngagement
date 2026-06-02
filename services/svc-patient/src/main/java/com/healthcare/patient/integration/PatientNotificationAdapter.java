package com.healthcare.patient.integration;

import com.healthcare.patient.domain.PatientProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class PatientNotificationAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PatientNotificationAdapter.class);

    private final RestClient restClient;
    private final String path;

    public PatientNotificationAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${platform.integration.notification.base-url:}") String baseUrl,
            @Value("${platform.integration.notification.path:/notifications}") String path) {
        this.restClient = baseUrl.isBlank() ? null : restClientBuilder.baseUrl(baseUrl).build();
        this.path = path;
    }

    public void sendRegistrationConfirmation(PatientProfile profile, String correlationId) {
        if (restClient == null) {
            LOGGER.warn("Skipping registration notification for aggregateId={} because base URL is not configured", profile.id());
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("recipientId", profile.externalReference());
        payload.put("channel", "EMAIL");
        payload.put("templateId", "PATIENT_REGISTRATION_CONFIRMED");
        payload.put("message", "Patient profile created successfully for " + profile.givenName() + " " + profile.familyName());

        restClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Correlation-Id", correlationId)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }
}
