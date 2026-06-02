package com.healthcare.patient.repository;

import com.healthcare.patient.domain.PatientProfile;

import java.util.List;
import java.util.Optional;

public interface PatientRepository {
    PatientProfile save(PatientProfile aggregate);
    Optional<PatientProfile> findById(String id);
    List<PatientProfile> findAll();
    boolean existsByExternalReference(String externalReference);
    boolean existsByEmail(String email);
    void deleteById(String id);
}
