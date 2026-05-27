package com.healthcare.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateNotificationRequest(
@jakarta.validation.constraints.NotBlank
String recipientId,
@jakarta.validation.constraints.NotBlank
String channel,
@jakarta.validation.constraints.NotBlank
String templateId,
@jakarta.validation.constraints.NotBlank
String message
) {
}
