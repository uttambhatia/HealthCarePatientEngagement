package com.healthcare.consent.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateConsentRequest(
@jakarta.validation.constraints.NotBlank
String patientId,
@jakarta.validation.constraints.NotBlank
String consentType,
boolean granted,
@jakarta.validation.constraints.NotBlank
String effectiveFrom
) {
}
