package com.healthcare.notification.event;

import com.healthcare.notification.dto.CreateNotificationRequest;
import com.healthcare.notification.port.AppointmentBookingPort;
import com.healthcare.notification.service.NotificationApplicationService;
import com.healthcare.platform.common.event.MessageEnvelope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {

    @Mock
    private NotificationApplicationService notificationService;

    @Mock
    private AppointmentBookingPort followUpAppointmentAdapter;

    private NotificationEventConsumer consumer() {
        return new NotificationEventConsumer(notificationService, followUpAppointmentAdapter, "SMS", "teleconsult-followup-v1");
    }

    @Test
    void shouldSendFollowUpNotificationWhenFollowUpIsRequired() {
        TeleconsultationCompletedEvent payload = new TeleconsultationCompletedEvent(
                "sess-1001",
                "apt-2001",
                "pat-3001",
                "prov-44",
                "2026-06-01T10:10:00Z",
                true,
                "2026-06-15T09:30:00Z"
        );

        MessageEnvelope<TeleconsultationCompletedEvent> envelope = new MessageEnvelope<>(
                "corr-5001",
                "TeleconsultationCompletedEvent",
                OffsetDateTime.now(),
                payload
        );

        consumer().handleTeleconsultationCompleted(envelope);

        ArgumentCaptor<CreateNotificationRequest> requestCaptor = ArgumentCaptor.forClass(CreateNotificationRequest.class);
        verify(notificationService).sendNotification(requestCaptor.capture(), eq("corr-5001"));

        CreateNotificationRequest request = requestCaptor.getValue();
        assertThat(request.recipientId()).isEqualTo("pat-3001");
        assertThat(request.channel()).isEqualTo("SMS");
        assertThat(request.templateId()).isEqualTo("teleconsult-followup-v1");
        assertThat(request.message()).contains("2026-06-15T09:30:00Z");
    }

    @Test
    void shouldCreateFollowUpAppointmentWhenFollowUpIsRequired() {
        TeleconsultationCompletedEvent payload = new TeleconsultationCompletedEvent(
                "sess-1001",
                "apt-2001",
                "pat-3001",
                "prov-44",
                "2026-06-01T10:10:00Z",
                true,
                "2026-06-15T09:30:00Z"
        );

        MessageEnvelope<TeleconsultationCompletedEvent> envelope = new MessageEnvelope<>(
                "corr-5001",
                "TeleconsultationCompletedEvent",
                OffsetDateTime.now(),
                payload
        );

        consumer().handleTeleconsultationCompleted(envelope);

        verify(followUpAppointmentAdapter).createFollowUpAppointment(
                eq("pat-3001"),
                eq("prov-44"),
                eq("2026-06-15T09:30:00Z"),
                eq("corr-5001")
        );
    }

    @Test
    void shouldSkipFollowUpActionsWhenFollowUpIsNotRequired() {
        TeleconsultationCompletedEvent payload = new TeleconsultationCompletedEvent(
                "sess-1002",
                "apt-2002",
                "pat-3002",
                "prov-77",
                "2026-06-01T11:10:00Z",
                false,
                ""
        );

        MessageEnvelope<TeleconsultationCompletedEvent> envelope = new MessageEnvelope<>(
                "corr-5002",
                "TeleconsultationCompletedEvent",
                OffsetDateTime.now(),
                payload
        );

        consumer().handleTeleconsultationCompleted(envelope);

        verify(notificationService, never()).sendNotification(any(), any());
        verify(followUpAppointmentAdapter, never()).createFollowUpAppointment(any(), any(), any(), any());
    }
}

