package com.healthcare.identityadapter.dto;

public record IdentityResponse(
        String id,
        String status,
                String subject,
String tenantId,
String role,
String tokenId
) {
}
