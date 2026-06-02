package com.healthcare.appointment.event;

import com.healthcare.platform.common.event.DomainEvent;

public record TeleconsultationCompletedEvent(
        String aggregateId,
        String appointmentId,
        String patientId,
        String providerId,
    String completedAt,
    boolean followUpRequired,
    String nextFollowUpDate
) implements DomainEvent {
    @Override
    public String eventType() {
        return "TeleconsultationCompletedEvent";
    }
}
