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

        if (!payload.followUpRequired()) {
            return;
        }

        String nextFollowUpDate = payload.nextFollowUpDate();
        String followUpMessage = (nextFollowUpDate == null || nextFollowUpDate.isBlank())
                ? "Your teleconsultation has been completed. Please schedule your follow-up appointment."
                : "Your teleconsultation has been completed. Please schedule your follow-up appointment for "
                + nextFollowUpDate + ".";

        notificationService.sendNotification(
                new CreateNotificationRequest(
                        payload.patientId(),
                        followUpChannel,
                        followUpTemplateId,
                        followUpMessage
                ),
                envelope.correlationId()
        );

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
}
