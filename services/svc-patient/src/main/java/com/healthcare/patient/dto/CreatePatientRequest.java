package com.healthcare.patient.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreatePatientRequest(
@jakarta.validation.constraints.NotBlank
String externalReference,
@jakarta.validation.constraints.NotBlank
String givenName,
@jakarta.validation.constraints.NotBlank
String familyName,
@jakarta.validation.constraints.NotBlank
String birthDate,
@NotBlank
@Email
String email,
@NotBlank
String phone,
@NotBlank
String demographics
) {
}
