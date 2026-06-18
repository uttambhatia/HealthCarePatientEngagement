package com.healthcare.identityadapter.dto;

import jakarta.validation.constraints.NotBlank;

public record AcsTeleconsultSessionRequest(
        @NotBlank String appointmentId,
        @NotBlank String patientId,
        @NotBlank String providerId) {
}