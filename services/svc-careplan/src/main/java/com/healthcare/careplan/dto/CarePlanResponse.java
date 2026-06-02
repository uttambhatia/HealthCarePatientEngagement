package com.healthcare.careplan.dto;

import java.util.List;

public record CarePlanResponse(
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
