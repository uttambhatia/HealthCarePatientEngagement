package com.healthcare.medicalrecord.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMedicalRecordRequest(
@jakarta.validation.constraints.NotBlank
String patientId,
@jakarta.validation.constraints.NotBlank
String fhirResourceType,
@jakarta.validation.constraints.NotBlank
String resourceReference,
@jakarta.validation.constraints.NotBlank
String summary
) {
}
