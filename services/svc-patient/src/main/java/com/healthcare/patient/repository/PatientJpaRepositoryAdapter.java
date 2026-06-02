package com.healthcare.patient.repository;

import com.healthcare.patient.domain.PatientProfile;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class PatientJpaRepositoryAdapter implements PatientRepository {
    private final JpaPatientEntityRepository jpaRepository;

    public PatientJpaRepositoryAdapter(JpaPatientEntityRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PatientProfile save(PatientProfile aggregate) {
        PatientProfileEntity saved = jpaRepository.save(toEntity(aggregate));
        return toDomain(saved);
    }

    @Override
    public Optional<PatientProfile> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<PatientProfile> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByExternalReference(String externalReference) {
        return jpaRepository.existsByExternalReferenceIgnoreCase(externalReference);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmailIgnoreCase(email);
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    private PatientProfileEntity toEntity(PatientProfile aggregate) {
        return new PatientProfileEntity(
                aggregate.id(),
                aggregate.status(),
                aggregate.externalReference(),
                aggregate.givenName(),
                aggregate.familyName(),
                aggregate.birthDate(),
                aggregate.email(),
                aggregate.phone(),
                aggregate.demographics()
        );
    }

    private PatientProfile toDomain(PatientProfileEntity entity) {
        return new PatientProfile(
                entity.getId(),
                entity.getStatus(),
                entity.getExternalReference(),
                entity.getGivenName(),
                entity.getFamilyName(),
                entity.getBirthDate(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getDemographics()
        );
    }
}