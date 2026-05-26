package com.healthcare.identityadapter.repository;

import com.healthcare.identityadapter.domain.IdentityAssertion;

import java.util.List;
import java.util.Optional;

public interface IdentityRepository {
    IdentityAssertion save(IdentityAssertion aggregate);
    Optional<IdentityAssertion> findById(String id);
    List<IdentityAssertion> findAll();
    void deleteById(String id);
}
