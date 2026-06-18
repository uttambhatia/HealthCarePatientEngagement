package com.healthcare.identityadapter.dto;

public record AcsTeleconsultSessionResponse(
        String sessionId,
        String doctorJoinUrl,
        String patientJoinUrl) {
}