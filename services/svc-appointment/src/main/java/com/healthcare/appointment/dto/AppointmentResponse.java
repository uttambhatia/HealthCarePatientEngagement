package com.healthcare.appointment.dto;

public record AppointmentResponse(
        String id,
        String status,
                String patientId,
String providerId,
String scheduledAt,
String channel
) {
}
