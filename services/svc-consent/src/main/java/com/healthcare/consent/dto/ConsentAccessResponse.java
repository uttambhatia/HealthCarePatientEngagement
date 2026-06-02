package com.healthcare.consent.dto;

public record ConsentAccessResponse(
        String patientId,
        String consentType,
        boolean accessAllowed,
        String reason,
        Integer latestVersion
) {
}
