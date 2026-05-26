package com.healthcare.alertmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAlertRequest(
@jakarta.validation.constraints.NotBlank
String patientId,
@jakarta.validation.constraints.NotBlank
String severity,
@jakarta.validation.constraints.NotBlank
String triggerType,
@jakarta.validation.constraints.NotBlank
String summary
) {
}
