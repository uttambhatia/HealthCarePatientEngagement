package com.healthcare.alertmanagement.event;

import com.healthcare.platform.common.event.DomainEvent;

public record AlertTriggeredEvent(
        String aggregateId,
    String patientId,
    String deviceId,
    String severity,
    String triggerType,
    String summary
) implements DomainEvent {
    @Override
    public String eventType() {
        return "AlertTriggeredEvent";
    }
}
