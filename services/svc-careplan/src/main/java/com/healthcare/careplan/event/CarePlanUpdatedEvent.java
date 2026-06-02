package com.healthcare.careplan.event;

import com.healthcare.platform.common.event.DomainEvent;

import java.util.List;

public record CarePlanUpdatedEvent(
        String aggregateId,
        String patientId,
        String goal,
        String planStatus,
        String ownerId,
        List<String> tasks,
        int version
) implements DomainEvent {
    @Override
    public String eventType() {
        return "CarePlanUpdatedEvent";
    }
}
