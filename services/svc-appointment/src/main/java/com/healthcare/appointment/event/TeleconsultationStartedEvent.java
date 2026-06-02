package com.healthcare.appointment.event;

import com.healthcare.platform.common.event.DomainEvent;

public record TeleconsultationStartedEvent(
        String aggregateId,
        String appointmentId,
        String patientId,
        String providerId,
        String startedAt
) implements DomainEvent {
    @Override
    public String eventType() {
        return "TeleconsultationStartedEvent";
    }
}
