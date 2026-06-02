package com.healthcare.consent.repository;

import com.healthcare.consent.domain.ConsentRecord;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class ConsentJpaRepositoryAdapter implements ConsentRepository {
    private final JpaConsentEntityRepository jpaRepository;

    public ConsentJpaRepositoryAdapter(JpaConsentEntityRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ConsentRecord save(ConsentRecord aggregate) {
        ConsentRecordEntity saved = jpaRepository.save(toEntity(aggregate));
        return toDomain(saved);
    }

    @Override
    public Optional<ConsentRecord> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<ConsentRecord> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<ConsentRecord> findLatestByPatientIdAndConsentType(String patientId, String consentType) {
        return jpaRepository.findTopByPatientIdAndConsentTypeOrderByVersionDesc(patientId, consentType).map(this::toDomain);
    }

    @Override
    public List<ConsentRecord> findHistoryByPatientIdAndConsentType(String patientId, String consentType) {
        return jpaRepository.findByPatientIdAndConsentTypeOrderByVersionDesc(patientId, consentType).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    private ConsentRecordEntity toEntity(ConsentRecord aggregate) {
        return new ConsentRecordEntity(
                aggregate.id(),
                aggregate.status(),
                aggregate.patientId(),
                aggregate.consentType(),
                aggregate.granted(),
                aggregate.version(),
                aggregate.effectiveFrom()
        );
    }

    private ConsentRecord toDomain(ConsentRecordEntity entity) {
        return new ConsentRecord(
                entity.getId(),
                entity.getStatus(),
                entity.getPatientId(),
                entity.getConsentType(),
                entity.isGranted(),
                entity.getVersion(),
                entity.getEffectiveFrom()
        );
    }
}