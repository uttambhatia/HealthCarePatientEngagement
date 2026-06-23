package com.healthcare.notification.event;

import com.healthcare.platform.common.event.DomainEvent;

public record FollowUpScheduledEvent(
        String aggregateId,
        String appointmentId,
        String patientId,
        String providerId,
        String scheduledAt,
        String status
) implements DomainEvent {
    @Override
    public String eventType() {
        return "FollowUpScheduledEvent";
    }
}
