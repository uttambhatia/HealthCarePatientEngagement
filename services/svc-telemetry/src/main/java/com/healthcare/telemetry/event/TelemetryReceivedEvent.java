package com.healthcare.telemetry.event;

import com.healthcare.platform.common.event.DomainEvent;

public record TelemetryReceivedEvent(
        String aggregateId,
String deviceId,
String metricType,
String metricValue,
String recordedAt
) implements DomainEvent {
    @Override
    public String eventType() {
        return "TelemetryReceivedEvent";
    }
}
