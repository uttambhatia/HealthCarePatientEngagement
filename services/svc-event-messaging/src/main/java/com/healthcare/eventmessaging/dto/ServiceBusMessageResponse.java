package com.healthcare.eventmessaging.dto;

public record ServiceBusMessageResponse(
        String id,
        String status,
        String channel,
        String eventName,
        String payload,
        String messageType,
        String recordedAt,
        String integrityHash,
        String anomalyReason
) {
}
