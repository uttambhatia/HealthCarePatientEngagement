package com.healthcare.medicalrecord.dto;

public record MedicalRecordResponse(
        String id,
        String status,
        String patientId,
        String fhirResourceType,
        String resourceReference,
        String summary,
        int version
) {
}
