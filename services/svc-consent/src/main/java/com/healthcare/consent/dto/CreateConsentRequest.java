package com.healthcare.consent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateConsentRequest(
@jakarta.validation.constraints.NotBlank
String patientId,
@jakarta.validation.constraints.NotBlank
String consentType,
@jakarta.validation.constraints.NotNull
boolean granted,
@jakarta.validation.constraints.NotBlank
String effectiveFrom
) {
}
