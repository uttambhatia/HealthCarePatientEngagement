package com.healthcare.alertmanagement.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAlertRequest(
@NotBlank
String patientId,
String deviceId,
@NotBlank
String severity,
@NotBlank
String triggerType,
String metricValue,
@NotBlank
String summary
) {
}
