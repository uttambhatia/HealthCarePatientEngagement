package com.healthcare.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePatientRequest(
@jakarta.validation.constraints.NotBlank
String externalReference,
@jakarta.validation.constraints.NotBlank
String givenName,
@jakarta.validation.constraints.NotBlank
String familyName,
@jakarta.validation.constraints.NotBlank
String birthDate
) {
}
