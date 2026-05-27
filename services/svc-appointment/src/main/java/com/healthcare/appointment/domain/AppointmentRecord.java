package com.healthcare.appointment.domain;

public record AppointmentRecord(
        String id,
        String status,
                String patientId,
String providerId,
String scheduledAt,
String channel
) {
}
