package com.healthcare.careplan.domain;

public record CarePlanAggregate(
        String id,
        String status,
                String patientId,
String goal,
String planStatus,
String ownerId
) {
}
