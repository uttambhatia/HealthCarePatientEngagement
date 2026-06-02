package com.healthcare.alertmanagement.dto;

public record AlertResponse(
        String id,
        String status,
        String patientId,
        String deviceId,
        String severity,
        String triggerType,
        String summary
) {
}
