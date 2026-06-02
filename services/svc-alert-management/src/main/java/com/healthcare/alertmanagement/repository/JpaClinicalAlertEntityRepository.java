package com.healthcare.alertmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaClinicalAlertEntityRepository extends JpaRepository<ClinicalAlertEntity, String> {
}
