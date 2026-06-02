package com.healthcare.consent.service;

import com.healthcare.consent.domain.ConsentRecord;
import com.healthcare.consent.dto.ConsentAccessResponse;
import com.healthcare.consent.dto.CreateConsentRequest;
import com.healthcare.consent.dto.ConsentResponse;
import com.healthcare.consent.event.ConsentUpdatedEvent;
import com.healthcare.consent.exception.ResourceNotFoundException;
import com.healthcare.consent.integration.ConsentAuditAdapter;
import com.healthcare.consent.repository.ConsentRepository;
import com.healthcare.platform.common.messaging.MessagingPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ConsentApplicationServiceImpl implements ConsentApplicationService {
    private final ConsentRepository repository;
    private final MessagingPort messagingPort;
    private final ConsentAuditAdapter integration;

    public ConsentApplicationServiceImpl(ConsentRepository repository, MessagingPort messagingPort, ConsentAuditAdapter integration) {
        this.repository = repository;
        this.messagingPort = messagingPort;
        this.integration = integration;
    }

    @Override
    public ConsentResponse recordConsent(CreateConsentRequest request, String correlationId) {
        int nextVersion = repository.findLatestByPatientIdAndConsentType(request.patientId(), request.consentType())
            .map(existing -> existing.version() + 1)
            .orElse(1);

        ConsentRecord aggregate = repository.save(new ConsentRecord(
                UUID.randomUUID().toString(),
            request.granted() ? "GRANTED" : "DENIED",
                request.patientId(),
        request.consentType(),
        request.granted(),
        nextVersion,
        request.effectiveFrom()
        ));
        integration.publishAuditTrail(aggregate, correlationId);
        messagingPort.publish("consent-service", correlationId, new ConsentUpdatedEvent(
                aggregate.id(),
                aggregate.patientId(),
                aggregate.consentType(),
                aggregate.granted(),
            aggregate.version(),
                aggregate.effectiveFrom()
        ));
        return map(aggregate);
    }

    @Override
    public ConsentResponse getConsent(String id) {
        return repository.findById(id).map(this::map)
                .orElseThrow(() -> new ResourceNotFoundException("Consent record not found: " + id));
    }

    @Override
    public List<ConsentResponse> listConsents() {
        return repository.findAll().stream().map(this::map).toList();
    }

    @Override
    public List<ConsentResponse> listConsentHistory(String patientId, String consentType) {
        return repository.findHistoryByPatientIdAndConsentType(patientId, consentType).stream().map(this::map).toList();
    }

    @Override
    public ConsentAccessResponse checkAccess(String patientId, String consentType) {
        return repository.findLatestByPatientIdAndConsentType(patientId, consentType)
                .map(consent -> {
                    if (consent.granted()) {
                        return new ConsentAccessResponse(patientId, consentType, true, "CONSENT_GRANTED", consent.version());
                    }
                    return new ConsentAccessResponse(patientId, consentType, false, "CONSENT_DENIED", consent.version());
                })
                .orElseGet(() -> new ConsentAccessResponse(patientId, consentType, false, "CONSENT_REQUIRED", null));
    }


    private ConsentResponse map(ConsentRecord aggregate) {
        return new ConsentResponse(
                aggregate.id(),
                aggregate.status(),
                aggregate.patientId(),
        aggregate.consentType(),
        aggregate.granted(),
            aggregate.version(),
        aggregate.effectiveFrom()
        );
    }
}
