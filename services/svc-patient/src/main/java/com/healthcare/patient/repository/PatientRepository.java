package com.healthcare.patient.repository;

import com.healthcare.patient.domain.PatientProfile;

import java.util.List;
import java.util.Optional;

public interface PatientRepository {
    PatientProfile save(PatientProfile aggregate);
    Optional<PatientProfile> findById(String id);
    List<PatientProfile> findAll();
    void deleteById(String id);
}
