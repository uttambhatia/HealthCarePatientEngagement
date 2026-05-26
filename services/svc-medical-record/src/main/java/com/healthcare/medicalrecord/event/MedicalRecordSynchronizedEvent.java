package com.healthcare.medicalrecord.event;

import com.healthcare.platform.common.event.DomainEvent;

public record MedicalRecordSynchronizedEvent(
        String aggregateId,
String patientId,
String fhirResourceType,
String resourceReference,
String summary
) implements DomainEvent {
    @Override
    public String eventType() {
        return "MedicalRecordSynchronizedEvent";
    }
}
