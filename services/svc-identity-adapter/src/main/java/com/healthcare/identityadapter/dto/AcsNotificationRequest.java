package com.healthcare.identityadapter.dto;

import jakarta.validation.constraints.NotBlank;

public record AcsNotificationRequest(
        @NotBlank String id,
        @NotBlank String status,
        @NotBlank String recipientId,
        @NotBlank String channel,
        @NotBlank String templateId,
        @NotBlank String message) {
}