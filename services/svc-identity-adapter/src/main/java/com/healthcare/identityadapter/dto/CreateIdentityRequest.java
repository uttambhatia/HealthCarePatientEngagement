package com.healthcare.identityadapter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateIdentityRequest(
@jakarta.validation.constraints.NotBlank
String subject,
@jakarta.validation.constraints.NotBlank
String tenantId,
@jakarta.validation.constraints.NotBlank
String role,
@jakarta.validation.constraints.NotBlank
String tokenId
) {
}
