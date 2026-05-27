package com.healthcare.medicalrecord.repository;

import com.healthcare.medicalrecord.domain.MedicalRecordSnapshot;

import java.util.List;
import java.util.Optional;

public interface MedicalRecordRepository {
    MedicalRecordSnapshot save(MedicalRecordSnapshot aggregate);
    Optional<MedicalRecordSnapshot> findById(String id);
    List<MedicalRecordSnapshot> findAll();
    void deleteById(String id);
}
