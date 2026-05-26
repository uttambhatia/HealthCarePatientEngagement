package com.healthcare.eventmessaging.event;

import com.healthcare.platform.common.event.DomainEvent;

public record ServiceBusMessageQueuedEvent(
        String aggregateId,
String channel,
String eventName,
String payload,
String messageType
) implements DomainEvent {
    @Override
    public String eventType() {
        return "ServiceBusMessageQueuedEvent";
    }
}
