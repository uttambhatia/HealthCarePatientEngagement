package com.healthcare.patient.dto;

public record PatientResponse(
        String id,
        String status,
                String externalReference,
String givenName,
String familyName,
String birthDate,
String email,
String phone,
String demographics
) {
}
