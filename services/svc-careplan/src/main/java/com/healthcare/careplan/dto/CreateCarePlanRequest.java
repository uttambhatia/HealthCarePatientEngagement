package com.healthcare.careplan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateCarePlanRequest(
@jakarta.validation.constraints.NotBlank
String patientId,
@jakarta.validation.constraints.NotBlank
String goal,
@jakarta.validation.constraints.NotBlank
String planStatus,
@jakarta.validation.constraints.NotBlank
String ownerId,
@NotEmpty
List<@NotBlank String> tasks
) {
}
