package com.healthcare.alertmanagement.event;

import com.healthcare.alertmanagement.dto.CreateAlertRequest;
import com.healthcare.alertmanagement.service.AlertApplicationService;
import com.healthcare.platform.common.event.MessageEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Evaluates completed teleconsultation notes for critical clinical findings and triggers
 * an alert when a configured keyword pattern is detected in the consultation notes.
 */
@Component
public class TeleconsultationAlertConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeleconsultationAlertConsumer.class);

    private final AlertApplicationService alertApplicationService;
    private final Pattern criticalKeywordPattern;

    public TeleconsultationAlertConsumer(
            AlertApplicationService alertApplicationService,
            @Value("${platform.alert.teleconsult.critical-keywords:critical|urgent|emergency|severe|worsening|deteriorating|abnormal}") String criticalKeywords) {
        this.alertApplicationService = alertApplicationService;
        this.criticalKeywordPattern = Pattern.compile(
                "\\b(" + criticalKeywords.replace(",", "|").trim() + ")\\b",
                Pattern.CASE_INSENSITIVE
        );
    }

    public void handle(MessageEnvelope<TeleconsultationCompletedEvent> envelope) {
        TeleconsultationCompletedEvent event = envelope.payload();
        LOGGER.info("Evaluating teleconsultation for clinical alerts correlationId={} appointmentId={} patientId={}",
                envelope.correlationId(), event.appointmentId(), event.patientId());

        // Evaluate keyword matches from the event note summary when present.
        // Full note text is still not propagated in events.
        String notesSummary = event.consultationNotesSummary();
        if (notesSummary != null && !notesSummary.isBlank() && criticalKeywordPattern.matcher(notesSummary).find()) {
            try {
                alertApplicationService.triggerAlert(
                        new CreateAlertRequest(
                                event.patientId(),
                                null,
                                "HIGH",
                                "TELECONSULT_CRITICAL_FINDINGS",
                                null,
                                "Critical findings detected in teleconsultation notes for appointment "
                                        + event.appointmentId() + ". Notes summary: " + notesSummary
                        ),
                        envelope.correlationId()
                );
                LOGGER.warn("Triggered TELECONSULT_CRITICAL_FINDINGS alert patientId={} appointmentId={} correlationId={}",
                        event.patientId(), event.appointmentId(), envelope.correlationId());
            } catch (Exception ex) {
                LOGGER.error("Failed to trigger teleconsult critical finding alert patientId={} appointmentId={} correlationId={} error={}",
                        event.patientId(), event.appointmentId(), envelope.correlationId(), ex.getMessage());
            }
        }

        // Trigger an informational alert when follow-up is required and no follow-up date was provided
        if (event.followUpRequired() && (event.nextFollowUpDate() == null || event.nextFollowUpDate().isBlank())) {
            try {
                alertApplicationService.triggerAlert(
                        new CreateAlertRequest(
                                event.patientId(),
                                null,
                                "WARNING",
                                "TELECONSULT_INCOMPLETE_FOLLOWUP",
                                null,
                                "Teleconsultation completed with follow-up required but no follow-up date was provided for appointment "
                                        + event.appointmentId()
                        ),
                        envelope.correlationId()
                );
                LOGGER.warn("Triggered TELECONSULT_INCOMPLETE_FOLLOWUP alert patientId={} appointmentId={} correlationId={}",
                        event.patientId(), event.appointmentId(), envelope.correlationId());
            } catch (Exception ex) {
                LOGGER.error("Failed to trigger teleconsult alert patientId={} appointmentId={} correlationId={} error={}",
                        event.patientId(), event.appointmentId(), envelope.correlationId(), ex.getMessage());
            }
        }
    }
}
