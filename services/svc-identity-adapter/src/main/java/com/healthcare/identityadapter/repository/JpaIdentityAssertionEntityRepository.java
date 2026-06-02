package com.healthcare.identityadapter.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaIdentityAssertionEntityRepository extends JpaRepository<IdentityAssertionEntity, String> {
}
