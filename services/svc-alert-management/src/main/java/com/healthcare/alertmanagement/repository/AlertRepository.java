package com.healthcare.alertmanagement.repository;

import com.healthcare.alertmanagement.domain.ClinicalAlert;

import java.util.List;
import java.util.Optional;

public interface AlertRepository {
    ClinicalAlert save(ClinicalAlert aggregate);
    Optional<ClinicalAlert> findById(String id);
    List<ClinicalAlert> findAll();
    void deleteById(String id);
}
