package com.healthcare.identityadapter.service;

import com.healthcare.identityadapter.dto.CreateIdentityRequest;
import com.healthcare.identityadapter.dto.IdentityResponse;

import java.util.List;

public interface IdentityApplicationService {
    IdentityResponse validateIdentity(CreateIdentityRequest request, String correlationId);
    IdentityResponse getAssertion(String id);
    List<IdentityResponse> listAssertions();
}
