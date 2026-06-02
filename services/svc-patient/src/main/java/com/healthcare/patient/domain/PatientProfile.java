package com.healthcare.patient.domain;

public record PatientProfile(
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
