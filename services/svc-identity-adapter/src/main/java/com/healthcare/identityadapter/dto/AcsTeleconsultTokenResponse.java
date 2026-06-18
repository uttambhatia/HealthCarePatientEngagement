package com.healthcare.identityadapter.dto;

public record AcsTeleconsultTokenResponse(
        String sessionId,
        String role,
        String accessToken,
        String tokenType,
        String expiresAt,
        String joinUrl,
        String tokenProvider) {
}
