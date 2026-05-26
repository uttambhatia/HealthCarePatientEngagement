package com.healthcare.consent.repository;

import com.healthcare.consent.domain.ConsentRecord;

import java.util.List;
import java.util.Optional;

public interface ConsentRepository {
    ConsentRecord save(ConsentRecord aggregate);
    Optional<ConsentRecord> findById(String id);
    List<ConsentRecord> findAll();
    void deleteById(String id);
}
