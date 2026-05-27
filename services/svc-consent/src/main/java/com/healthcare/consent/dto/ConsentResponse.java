package com.healthcare.consent.dto;

public record ConsentResponse(
        String id,
        String status,
                String patientId,
String consentType,
String granted,
String effectiveFrom
) {
}
