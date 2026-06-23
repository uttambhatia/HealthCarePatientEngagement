package com.healthcare.notification.event;

public record TeleconsultationCompletedEvent(
        String aggregateId,
        String appointmentId,
        String patientId,
        String providerId,
        String completedAt,
        boolean followUpRequired,
        String nextFollowUpDate,
        String consultationNotesSummary
) {
}
