package com.healthcare.identityadapter.domain;

public record IdentityAssertion(
        String id,
        String status,
                String subject,
String tenantId,
String role,
String tokenId
) {
}
