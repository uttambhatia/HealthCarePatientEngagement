package com.healthcare.appointment.domain;

import java.util.List;

public record TeleconsultationSession(
        String id,
        String appointmentId,
        String patientId,
        String providerId,
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
