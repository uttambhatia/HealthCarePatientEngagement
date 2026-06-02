package com.healthcare.careplan.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaCarePlanEntityRepository extends JpaRepository<CarePlanAggregateEntity, String> {
	Optional<CarePlanAggregateEntity> findTopByPatientIdOrderByVersionDesc(String patientId);
}