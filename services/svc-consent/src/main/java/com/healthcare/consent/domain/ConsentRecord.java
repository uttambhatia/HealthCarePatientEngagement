package com.healthcare.consent.domain;

public record ConsentRecord(
        String id,
        String status,
                String patientId,
String consentType,
String granted,
String effectiveFrom
) {
}
