package com.healthcare.notification.domain;

public record NotificationJob(
        String id,
        String status,
                String recipientId,
String channel,
String templateId,
String message
) {
}
