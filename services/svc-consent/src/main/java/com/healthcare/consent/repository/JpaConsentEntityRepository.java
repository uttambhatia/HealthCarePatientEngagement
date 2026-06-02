package com.healthcare.consent.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaConsentEntityRepository extends JpaRepository<ConsentRecordEntity, String> {
	Optional<ConsentRecordEntity> findTopByPatientIdAndConsentTypeOrderByVersionDesc(String patientId, String consentType);
	List<ConsentRecordEntity> findByPatientIdAndConsentTypeOrderByVersionDesc(String patientId, String consentType);
}