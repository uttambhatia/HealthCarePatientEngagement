package com.healthcare.consent.dto;

public record ConsentResponse(
        String id,
        String status,
        String patientId,
String consentType,
boolean granted,
int version,
String effectiveFrom
) {
}
