package com.healthcare.eventmessaging.event;

import com.healthcare.platform.common.event.DomainEvent;

public record MonitoringAnomalyDetectedEvent(
        String aggregateId,
        String channel,
        String eventName,
        String messageType,
        String anomalyReason
) implements DomainEvent {
    @Override
    public String eventType() {
        return "MonitoringAnomalyDetectedEvent";
    }
}
