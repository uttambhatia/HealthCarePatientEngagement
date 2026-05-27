package com.healthcare.notification.event;

import com.healthcare.platform.common.event.DomainEvent;

public record NotificationSentEvent(
        String aggregateId,
String recipientId,
String channel,
String templateId,
String message
) implements DomainEvent {
    @Override
    public String eventType() {
        return "NotificationSentEvent";
    }
}
