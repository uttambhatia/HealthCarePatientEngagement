package com.healthcare.consent.domain;

public record ConsentRecord(
        String id,
        String status,
        String patientId,
String consentType,
boolean granted,
int version,
String effectiveFrom
) {
}
