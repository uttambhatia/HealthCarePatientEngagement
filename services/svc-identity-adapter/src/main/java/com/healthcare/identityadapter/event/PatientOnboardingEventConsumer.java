package com.healthcare.identityadapter.event;

import com.healthcare.platform.common.event.MessageEnvelope;
import com.healthcare.identityadapter.service.EntraProvisioningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PatientOnboardingEventConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PatientOnboardingEventConsumer.class);

    private final EntraProvisioningService provisioningService;

    public PatientOnboardingEventConsumer(EntraProvisioningService provisioningService) {
        this.provisioningService = provisioningService;
    }

    public void handle(MessageEnvelope<PatientOnboardingRequestedEvent> envelope) {
        LOGGER.info("Consumed onboarding eventName={} correlationId={} externalReference={} targetRole={}",
                envelope.eventName(),
                envelope.correlationId(),
                envelope.payload().externalReference(),
                envelope.payload().targetRole());

        provisioningService.provisionFromApproval(envelope.payload(), envelope.correlationId());
    }
}