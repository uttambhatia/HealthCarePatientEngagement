package com.healthcare.careplan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCarePlanRequest(
@jakarta.validation.constraints.NotBlank
String patientId,
@jakarta.validation.constraints.NotBlank
String goal,
@jakarta.validation.constraints.NotBlank
String status,
@jakarta.validation.constraints.NotBlank
String ownerId
) {
}
