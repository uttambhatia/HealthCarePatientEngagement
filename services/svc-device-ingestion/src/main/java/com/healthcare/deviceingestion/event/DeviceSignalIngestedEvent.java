package com.healthcare.deviceingestion.event;

import com.healthcare.platform.common.event.DomainEvent;

public record DeviceSignalIngestedEvent(
        String aggregateId,
String deviceId,
String protocol,
String payload,
String receivedAt
) implements DomainEvent {
    @Override
    public String eventType() {
        return "DeviceSignalIngestedEvent";
    }
}
