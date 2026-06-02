package com.healthcare.patient.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPatientEntityRepository extends JpaRepository<PatientProfileEntity, String> {
	boolean existsByExternalReferenceIgnoreCase(String externalReference);
	boolean existsByEmailIgnoreCase(String email);
}