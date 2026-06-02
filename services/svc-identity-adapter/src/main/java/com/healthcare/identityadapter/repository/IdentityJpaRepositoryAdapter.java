package com.healthcare.identityadapter.repository;

import com.healthcare.identityadapter.domain.IdentityAssertion;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class IdentityJpaRepositoryAdapter implements IdentityRepository {
    private final JpaIdentityAssertionEntityRepository jpaRepository;

    public IdentityJpaRepositoryAdapter(JpaIdentityAssertionEntityRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public IdentityAssertion save(IdentityAssertion aggregate) {
        IdentityAssertionEntity saved = jpaRepository.save(toEntity(aggregate));
        return toDomain(saved);
    }

    @Override
    public Optional<IdentityAssertion> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<IdentityAssertion> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    private IdentityAssertionEntity toEntity(IdentityAssertion aggregate) {
        return new IdentityAssertionEntity(
                aggregate.id(),
                aggregate.status(),
                aggregate.subject(),
                aggregate.tenantId(),
                aggregate.role(),
                aggregate.tokenId()
        );
    }

    private IdentityAssertion toDomain(IdentityAssertionEntity entity) {
        return new IdentityAssertion(
                entity.getId(),
                entity.getStatus(),
                entity.getSubject(),
                entity.getTenantId(),
                entity.getRole(),
                entity.getTokenId()
        );
    }
}
