package com.healthcare.careplan.event;

import com.healthcare.platform.common.event.DomainEvent;

public record CarePlanCreatedEvent(
        String aggregateId,
String patientId,
String goal,
String planStatus,
String ownerId
) implements DomainEvent {
    @Override
    public String eventType() {
        return "CarePlanCreatedEvent";
    }
}
