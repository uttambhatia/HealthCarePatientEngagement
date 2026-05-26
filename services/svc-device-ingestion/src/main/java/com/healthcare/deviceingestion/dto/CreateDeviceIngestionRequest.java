package com.healthcare.deviceingestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDeviceIngestionRequest(
@jakarta.validation.constraints.NotBlank
String deviceId,
@jakarta.validation.constraints.NotBlank
String protocol,
@jakarta.validation.constraints.NotBlank
String payload,
@jakarta.validation.constraints.NotBlank
String receivedAt
) {
}
