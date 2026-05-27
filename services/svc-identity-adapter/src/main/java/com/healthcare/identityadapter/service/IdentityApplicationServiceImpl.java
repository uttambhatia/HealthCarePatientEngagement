package com.healthcare.identityadapter.service;

import com.healthcare.identityadapter.domain.IdentityAssertion;
import com.healthcare.identityadapter.dto.CreateIdentityRequest;
import com.healthcare.identityadapter.dto.IdentityResponse;
import com.healthcare.identityadapter.event.IdentityValidatedEvent;
import com.healthcare.identityadapter.exception.ResourceNotFoundException;
import com.healthcare.identityadapter.integration.AzureAdAdapter;
import com.healthcare.identityadapter.repository.IdentityRepository;
import com.healthcare.platform.common.messaging.MessagingPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class IdentityApplicationServiceImpl implements IdentityApplicationService {
    private final IdentityRepository repository;
    private final MessagingPort messagingPort;
    private final AzureAdAdapter integration;

    public IdentityApplicationServiceImpl(IdentityRepository repository, MessagingPort messagingPort, AzureAdAdapter integration) {
        this.repository = repository;
        this.messagingPort = messagingPort;
        this.integration = integration;
    }

    @Override
    public IdentityResponse validateIdentity(CreateIdentityRequest request, String correlationId) {
        IdentityAssertion aggregate = repository.save(new IdentityAssertion(
                UUID.randomUUID().toString(),
                "VALIDATED",
                request.subject(),
        request.tenantId(),
        request.role(),
        request.tokenId()
        ));
        integration.validateAccessToken(aggregate, correlationId);
        messagingPort.publish("identity-adapter-service", correlationId, new IdentityValidatedEvent(
                aggregate.id(),
                aggregate.subject(),
                aggregate.tenantId(),
                aggregate.role(),
                aggregate.tokenId()
        ));
        return map(aggregate);
    }

    @Override
    public IdentityResponse getAssertion(String id) {
        return repository.findById(id).map(this::map)
                .orElseThrow(() -> new ResourceNotFoundException("IdentityAdapter record not found: " + id));
    }

    @Override
    public List<IdentityResponse> listAssertions() {
        return repository.findAll().stream().map(this::map).toList();
    }


    private IdentityResponse map(IdentityAssertion aggregate) {
        return new IdentityResponse(
                aggregate.id(),
                aggregate.status(),
                aggregate.subject(),
        aggregate.tenantId(),
        aggregate.role(),
        aggregate.tokenId()
        );
    }
}
