package com.healthcare.consent.service;

import com.healthcare.consent.domain.ConsentRecord;
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
        ConsentRecord aggregate = repository.save(new ConsentRecord(
                UUID.randomUUID().toString(),
                "RECORDED",
                request.patientId(),
        request.consentType(),
        String.valueOf(request.granted()),
        request.effectiveFrom()
        ));
        integration.publishAuditTrail(aggregate, correlationId);
        messagingPort.publish("consent-service", correlationId, new ConsentUpdatedEvent(
                aggregate.id(),
                aggregate.patientId(),
                aggregate.consentType(),
                aggregate.granted(),
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


    private ConsentResponse map(ConsentRecord aggregate) {
        return new ConsentResponse(
                aggregate.id(),
                aggregate.status(),
                aggregate.patientId(),
        aggregate.consentType(),
        aggregate.granted(),
        aggregate.effectiveFrom()
        );
    }
}
