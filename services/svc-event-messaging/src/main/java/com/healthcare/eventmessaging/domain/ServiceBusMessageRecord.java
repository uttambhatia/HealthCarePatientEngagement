package com.healthcare.eventmessaging.domain;

public record ServiceBusMessageRecord(
        String id,
        String status,
                String channel,
String eventName,
String payload,
String messageType
) {
}
