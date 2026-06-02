package com.healthcare.medicalrecord.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaMedicalRecordEntityRepository extends JpaRepository<MedicalRecordSnapshotEntity, String> {
}