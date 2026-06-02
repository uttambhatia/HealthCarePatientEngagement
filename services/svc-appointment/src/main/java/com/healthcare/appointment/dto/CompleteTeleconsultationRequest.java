package com.healthcare.appointment.dto;

import jakarta.validation.constraints.NotBlank;

public record CompleteTeleconsultationRequest(
        @NotBlank String consultationNotes,
        boolean followUpRequired,
        String nextFollowUpDate
) {
}
