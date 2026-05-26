package com.healthcare.consent.event;

import com.healthcare.platform.common.event.DomainEvent;

public record ConsentUpdatedEvent(
        String aggregateId,
String patientId,
String consentType,
String granted,
String effectiveFrom
) implements DomainEvent {
    @Override
    public String eventType() {
        return "ConsentUpdatedEvent";
    }
}
