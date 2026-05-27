package com.healthcare.notification.dto;

public record NotificationResponse(
        String id,
        String status,
                String recipientId,
String channel,
String templateId,
String message
) {
}
