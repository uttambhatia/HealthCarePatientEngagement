package com.healthcare.appointment.event;

import com.healthcare.platform.common.event.DomainEvent;

public record AppointmentBookedEvent(
        String aggregateId,
String patientId,
String providerId,
String scheduledAt,
String channel
) implements DomainEvent {
    @Override
    public String eventType() {
        return "AppointmentBookedEvent";
    }
}
