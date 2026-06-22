package com.healthcare.appointment.integration;

import com.healthcare.appointment.domain.TeleconsultationSession;
import com.healthcare.appointment.exception.TeleconsultationNetworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TeleconsultationMedicalRecordAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeleconsultationMedicalRecordAdapter.class);

    private final RestClient restClient;
    private final String path;
    private final String fhirResourceType;
    private final int maxAttempts;

    public TeleconsultationMedicalRecordAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${platform.integration.medicalRecord.base-url:}") String baseUrl,
            @Value("${platform.integration.medicalRecord.path:/medical-records}") String path,
            @Value("${platform.integration.medicalRecord.fhirResourceType:Encounter}") String fhirResourceType,
            @Value("${platform.messaging.retryAttempts:3}") int maxAttempts) {
        this.restClient = baseUrl.isBlank() ? null : restClientBuilder.baseUrl(baseUrl).build();
        this.path = path;
        this.fhirResourceType = fhirResourceType;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    public void syncConsultationNotes(TeleconsultationSession session, String consultationNotes, List<String> prescriptions, String correlationId) {
        if (restClient == null) {
            LOGGER.warn("Skipping teleconsultation medical-record integration for sessionId={} because base URL is not configured", session.id());
            return;
        }

        // Primary Encounter record
        sendRecord(session, fhirResourceType, consultationNotes, correlationId);

        // MedicationRequest record when prescriptions are present
        if (prescriptions != null && !prescriptions.isEmpty()) {
            String medicationSummary = String.join("; ", prescriptions);
            sendRecord(session, "MedicationRequest", medicationSummary, correlationId);
        }

        // CarePlan record when follow-up is required
        if (session.followUpRequired() && session.nextFollowUpDate() != null && !session.nextFollowUpDate().isBlank()) {
            String carePlanSummary = "Follow-up scheduled for " + session.nextFollowUpDate();
            sendRecord(session, "CarePlan", carePlanSummary, correlationId);
        }
    }

    private void sendRecord(TeleconsultationSession session, String resourceType, String summary, String correlationId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("patientId", session.patientId());
        payload.put("fhirResourceType", resourceType);
        payload.put("resourceReference", "Appointment/" + session.appointmentId() + "/Teleconsultation/" + session.id());
        payload.put("summary", summary);

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
                LOGGER.info("Teleconsultation medical-record integration succeeded resourceType={} sessionId={} correlationId={} attempt={}", resourceType, session.id(), correlationId, attempt);
                return;
            } catch (RuntimeException ex) {
                lastError = ex;
                LOGGER.warn("Teleconsultation medical-record integration attempt failed resourceType={} sessionId={} correlationId={} attempt={} maxAttempts={} error={}",
                        resourceType, session.id(), correlationId, attempt, maxAttempts, ex.getMessage());
            }
        }

        throw new TeleconsultationNetworkException(
                "Medical record synchronization failed for teleconsultation sessionId=" + session.id() + " resourceType=" + resourceType,
                lastError
        );
    }
}
