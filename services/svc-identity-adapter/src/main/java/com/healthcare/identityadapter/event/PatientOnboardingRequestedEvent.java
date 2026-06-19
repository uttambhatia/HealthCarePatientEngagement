package com.healthcare.identityadapter.event;

import com.healthcare.platform.common.event.DomainEvent;

public record PatientOnboardingRequestedEvent(
        String aggregateId,
        String externalReference,
        String givenName,
        String familyName,
        String email,
        String targetRole
) implements DomainEvent {
    @Override
    public String eventType() {
        return "PatientOnboardingRequestedEvent";
    }
}