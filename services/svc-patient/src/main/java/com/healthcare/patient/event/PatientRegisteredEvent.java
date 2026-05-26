package com.healthcare.patient.event;

import com.healthcare.platform.common.event.DomainEvent;

public record PatientRegisteredEvent(
        String aggregateId,
String externalReference,
String givenName,
String familyName,
String birthDate
) implements DomainEvent {
    @Override
    public String eventType() {
        return "PatientRegisteredEvent";
    }
}
