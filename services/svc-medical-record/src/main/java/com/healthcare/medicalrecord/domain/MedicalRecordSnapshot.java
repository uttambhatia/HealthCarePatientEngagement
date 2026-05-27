package com.healthcare.medicalrecord.domain;

public record MedicalRecordSnapshot(
        String id,
        String status,
                String patientId,
String fhirResourceType,
String resourceReference,
String summary
) {
}
