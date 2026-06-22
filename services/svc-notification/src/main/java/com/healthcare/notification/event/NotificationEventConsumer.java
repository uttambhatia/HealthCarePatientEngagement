package com.healthcare.notification.event;

import com.healthcare.notification.dto.CreateNotificationRequest;
import com.healthcare.notification.port.AppointmentBookingPort;
import com.healthcare.notification.service.NotificationApplicationService;
import com.healthcare.platform.common.event.MessageEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationEventConsumer.class);

    private final NotificationApplicationService notificationService;
    private final AppointmentBookingPort followUpAppointmentAdapter;
    private final String followUpChannel;
    private final String followUpTemplateId;

    public NotificationEventConsumer(
            NotificationApplicationService notificationService,
            AppointmentBookingPort followUpAppointmentAdapter,
            @Value("${platform.teleconsult.followup.notification.channel:SMS}") String followUpChannel,
            @Value("${platform.teleconsult.followup.notification.templateId:teleconsult-followup-v1}") String followUpTemplateId) {
        this.notificationService = notificationService;
        this.followUpAppointmentAdapter = followUpAppointmentAdapter;
        this.followUpChannel = followUpChannel;
        this.followUpTemplateId = followUpTemplateId;
    }

    public void handleNotificationSent(MessageEnvelope<NotificationSentEvent> envelope) {
        LOGGER.info("Consumed eventName={} correlationId={} aggregateId={}",
                envelope.eventName(), envelope.correlationId(), envelope.payload().aggregateId());
    }

    public void handleTeleconsultationCompleted(MessageEnvelope<TeleconsultationCompletedEvent> envelope) {
        TeleconsultationCompletedEvent payload = envelope.payload();
        LOGGER.info("Consumed eventName={} correlationId={} appointmentId={} followUpRequired={}",
                envelope.eventName(), envelope.correlationId(), payload.appointmentId(), payload.followUpRequired());

        // Always send patient a consultation summary notification
        String summaryMessage = payload.followUpRequired() && payload.nextFollowUpDate() != null && !payload.nextFollowUpDate().isBlank()
                ? "Your teleconsultation has been completed. Your follow-up is scheduled for " + payload.nextFollowUpDate() + ". Please check your care plan for details."
                : "Your teleconsultation has been completed. Your doctor has recorded consultation notes. Please contact your care coordinator if you have questions.";

        notificationService.sendNotification(
                new CreateNotificationRequest(
                        payload.patientId(),
                        followUpChannel,
                        "teleconsult-summary-v1",
                        summaryMessage
                ),
                envelope.correlationId()
        );

        // Follow-up appointment creation when follow-up is required
        if (payload.followUpRequired()) {
            String nextFollowUpDate = payload.nextFollowUpDate();
            if (nextFollowUpDate != null && !nextFollowUpDate.isBlank()) {
                try {
                    followUpAppointmentAdapter.createFollowUpAppointment(
                            payload.patientId(),
                            payload.providerId(),
                            nextFollowUpDate,
                            envelope.correlationId()
                    );
                } catch (Exception ex) {
                    LOGGER.error("Failed to create follow-up appointment draft appointmentId={} patientId={} correlationId={} error={}",
                            payload.appointmentId(), payload.patientId(), envelope.correlationId(), ex.getMessage());
                }
            } else {
                LOGGER.warn("Follow-up appointment not scheduled because nextFollowUpDate is blank appointmentId={} correlationId={}",
                        payload.appointmentId(), envelope.correlationId());
            }
        }

        // Notify care coordinator
        try {
            notificationService.sendNotification(
                    new CreateNotificationRequest(
                            payload.providerId(),
                            followUpChannel,
                            "teleconsult-coordinator-update-v1",
                            "Teleconsultation completed for patient " + payload.patientId()
                                    + ". Appointment: " + payload.appointmentId()
                                    + (payload.followUpRequired() ? ". Follow-up required: " + payload.nextFollowUpDate() : "")
                                    + ". Please review care plan and assign any follow-up tasks."
                    ),
                    envelope.correlationId()
            );
        } catch (Exception ex) {
            LOGGER.warn("Failed to send coordinator teleconsult completion notification providerId={} correlationId={} error={}",
                    payload.providerId(), envelope.correlationId(), ex.getMessage());
        }
    }
}
