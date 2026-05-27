package com.healthcare.careplan.dto;

public record CarePlanResponse(
        String id,
        String status,
                String patientId,
String goal,
String planStatus,
String ownerId
) {
}
