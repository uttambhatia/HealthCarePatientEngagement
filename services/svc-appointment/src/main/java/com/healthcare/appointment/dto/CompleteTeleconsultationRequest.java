package com.healthcare.appointment.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CompleteTeleconsultationRequest(
        @NotBlank String consultationNotes,
        boolean followUpRequired,
        String nextFollowUpDate,
        List<String> prescriptions
) {
}
