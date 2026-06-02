package com.healthcare.medicalrecord.exception;

public class FhirValidationException extends RuntimeException {
    public FhirValidationException(String fhirResourceType) {
        super("Unsupported FHIR resource type: " + fhirResourceType);
    }
}
