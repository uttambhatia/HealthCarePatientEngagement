package com.healthcare.eventmessaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateServiceBusMessageRequest(
@jakarta.validation.constraints.NotBlank
String channel,
@jakarta.validation.constraints.NotBlank
String eventName,
@jakarta.validation.constraints.NotBlank
String payload,
@jakarta.validation.constraints.NotBlank
String messageType
) {
}
