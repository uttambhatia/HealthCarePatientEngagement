package com.healthcare.patient.domain;

public record PatientProfile(
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
String idProofBlobName,
String idProofFileName
) {
}
