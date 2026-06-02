package com.healthcare.alertmanagement.domain;

public record ClinicalAlert(
        String id,
        String status,
        String patientId,
        String deviceId,
        String severity,
        String triggerType,
        String summary
) {
}
