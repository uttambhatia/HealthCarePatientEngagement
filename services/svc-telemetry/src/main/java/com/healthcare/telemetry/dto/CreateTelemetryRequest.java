package com.healthcare.telemetry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTelemetryRequest(
@jakarta.validation.constraints.NotBlank
String deviceId,
@jakarta.validation.constraints.NotBlank
String metricType,
@jakarta.validation.constraints.NotBlank
String metricValue,
@jakarta.validation.constraints.NotBlank
String recordedAt
) {
}
