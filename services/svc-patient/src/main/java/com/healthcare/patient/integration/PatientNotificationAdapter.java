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
    private static final String STATUS_PENDING_VERIFICATION = "PENDING_VERIFICATION";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final RestClient restClient;
    private final String path;

    public PatientNotificationAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${platform.integration.notification.base-url:}") String baseUrl,
            @Value("${platform.integration.notification.path:/notifications}") String path) {
        this.restClient = baseUrl.isBlank() ? null : restClientBuilder.baseUrl(baseUrl).build();
        this.path = path;
    }

    public void sendPendingVerification(PatientProfile profile, String correlationId) {
        if (restClient == null) {
            LOGGER.warn("Skipping registration notification for aggregateId={} because base URL is not configured", profile.id());
            return;
        }

        dispatchToChannels(
                profile,
                "PATIENT_REGISTRATION_PENDING_VERIFICATION",
                "Your registration request has been received and is pending coordinator verification.",
                "PATIENT_REGISTRATION_PENDING_VERIFICATION_SMS",
                "Registration received and pending coordinator verification.",
                correlationId);
    }

    public void sendApprovalNotification(PatientProfile profile, String correlationId) {
        if (restClient == null) {
            LOGGER.warn("Skipping approval notification for aggregateId={} because base URL is not configured", profile.id());
            return;
        }

        dispatchToChannels(
                profile,
                "PATIENT_REGISTRATION_APPROVED",
                "Your patient registration has been approved. You can now access care services.",
                "PATIENT_REGISTRATION_APPROVED_SMS",
                "Your patient registration is approved.",
                correlationId);
    }

    public void sendRejectionNotification(PatientProfile profile, String correlationId) {
        if (restClient == null) {
            LOGGER.warn("Skipping rejection notification for aggregateId={} because base URL is not configured", profile.id());
            return;
        }

        dispatchToChannels(
                profile,
                "PATIENT_REGISTRATION_REJECTED",
                "Your patient registration was rejected. Please contact your care coordinator for details.",
                "PATIENT_REGISTRATION_REJECTED_SMS",
                "Your patient registration was rejected. Contact your care coordinator.",
                correlationId);
    }

    public void resendNotification(PatientProfile profile, String correlationId) {
        String status = profile.status() == null ? "" : profile.status().trim().toUpperCase();
        if (STATUS_COMPLETED.equals(status)) {
            sendApprovalNotification(profile, correlationId);
            return;
        }
        if (STATUS_REJECTED.equals(status)) {
            sendRejectionNotification(profile, correlationId);
            return;
        }
        if (STATUS_PENDING_VERIFICATION.equals(status)) {
            sendPendingVerification(profile, correlationId);
            return;
        }

        LOGGER.warn("Skipping notification resend for aggregateId={} because status={} is unsupported", profile.id(), profile.status());
    }

    private void dispatchToChannels(
            PatientProfile profile,
            String emailTemplate,
            String emailMessage,
            String smsTemplate,
            String smsMessage,
            String correlationId) {
        dispatch(profile.email(), "EMAIL", emailTemplate, emailMessage, profile, correlationId);

        if (profile.phone() != null && !profile.phone().isBlank()) {
            dispatch(profile.phone(), "SMS", smsTemplate, smsMessage, profile, correlationId);
        } else {
            LOGGER.warn("Skipping SMS registration notification for aggregateId={} because phone is missing", profile.id());
        }
    }

    private void dispatch(
            String recipientId,
            String channel,
            String templateId,
            String message,
            PatientProfile profile,
            String correlationId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("recipientId", recipientId);
        payload.put("channel", channel);
        payload.put("templateId", templateId);
        payload.put("message", message + " Patient: " + profile.givenName() + " " + profile.familyName());

        restClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Correlation-Id", correlationId)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }
}
