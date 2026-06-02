package com.healthcare.careplan.dto;

public record CarePlanResponsibilityResponse(
        String patientId,
        String ownerId,
        String carePlanId,
        String planStatus,
        int version
) {
}
