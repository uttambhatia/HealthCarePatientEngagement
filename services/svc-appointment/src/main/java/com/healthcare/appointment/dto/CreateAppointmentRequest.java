package com.healthcare.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAppointmentRequest(
@jakarta.validation.constraints.NotBlank
String patientId,
@jakarta.validation.constraints.NotBlank
String providerId,
@jakarta.validation.constraints.NotBlank
String scheduledAt,
@jakarta.validation.constraints.NotBlank
String channel
) {
}
