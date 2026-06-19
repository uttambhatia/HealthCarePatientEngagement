package com.healthcare.patient.dto;

public record PatientResponse(
        String id,
        String status,
        String decisionAudit,
        String externalReference,
String givenName,
String familyName,
String birthDate,
String email,
String phone,
String demographics,
boolean idProofUploaded,
String idProofFileName
) {
}
