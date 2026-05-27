package com.healthcare.alertmanagement.domain;

public record ClinicalAlert(
        String id,
        String status,
                String patientId,
String severity,
String triggerType,
String summary
) {
}
