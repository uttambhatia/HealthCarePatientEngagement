package com.healthcare.appointment.dto;

import java.util.List;

public record TeleconsultationResponse(
        String sessionId,
        String appointmentId,
        String status,
        String doctorJoinUrl,
        String patientJoinUrl,
        String startedAt,
        String joinedAt,
        String completedAt,
        String consultationNotes,
        boolean followUpRequired,
        String nextFollowUpDate,
        List<String> interactionLogs
) {
}
