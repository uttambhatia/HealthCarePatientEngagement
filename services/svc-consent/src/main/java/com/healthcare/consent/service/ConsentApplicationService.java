package com.healthcare.consent.service;

import com.healthcare.consent.dto.CreateConsentRequest;
import com.healthcare.consent.dto.ConsentAccessResponse;
import com.healthcare.consent.dto.ConsentResponse;

import java.util.List;

public interface ConsentApplicationService {
    ConsentResponse recordConsent(CreateConsentRequest request, String correlationId);
    ConsentResponse getConsent(String id);
    List<ConsentResponse> listConsents();
    List<ConsentResponse> listConsentHistory(String patientId, String consentType);
    ConsentAccessResponse checkAccess(String patientId, String consentType);
}
