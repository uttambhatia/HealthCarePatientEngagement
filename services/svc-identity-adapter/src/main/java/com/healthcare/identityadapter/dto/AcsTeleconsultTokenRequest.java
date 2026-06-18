package com.healthcare.identityadapter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AcsTeleconsultTokenRequest(
        @NotBlank String sessionId,
        @NotBlank @Pattern(regexp = "DOCTOR|PATIENT", message = "role must be DOCTOR or PATIENT") String role) {
}
