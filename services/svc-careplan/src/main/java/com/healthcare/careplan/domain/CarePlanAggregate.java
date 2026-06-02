package com.healthcare.careplan.domain;

import java.util.List;

public record CarePlanAggregate(
        String id,
        String status,
        String patientId,
        String goal,
        String planStatus,
        String ownerId,
        List<String> tasks,
        int version
) {
}
