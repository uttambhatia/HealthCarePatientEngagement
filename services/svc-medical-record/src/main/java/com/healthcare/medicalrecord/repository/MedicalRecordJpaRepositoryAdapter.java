package com.healthcare.medicalrecord.repository;

import com.healthcare.medicalrecord.domain.MedicalRecordSnapshot;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class MedicalRecordJpaRepositoryAdapter implements MedicalRecordRepository {
    private final JpaMedicalRecordEntityRepository jpaRepository;

    public MedicalRecordJpaRepositoryAdapter(JpaMedicalRecordEntityRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public MedicalRecordSnapshot save(MedicalRecordSnapshot aggregate) {
        MedicalRecordSnapshotEntity saved = jpaRepository.save(toEntity(aggregate));
        return toDomain(saved);
    }

    @Override
    public Optional<MedicalRecordSnapshot> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<MedicalRecordSnapshot> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    private MedicalRecordSnapshotEntity toEntity(MedicalRecordSnapshot aggregate) {
        return new MedicalRecordSnapshotEntity(
                aggregate.id(),
                aggregate.status(),
                aggregate.patientId(),
                aggregate.fhirResourceType(),
                aggregate.resourceReference(),
            aggregate.summary(),
            aggregate.version()
        );
    }

    private MedicalRecordSnapshot toDomain(MedicalRecordSnapshotEntity entity) {
        return new MedicalRecordSnapshot(
                entity.getId(),
                entity.getStatus(),
                entity.getPatientId(),
                entity.getFhirResourceType(),
                entity.getResourceReference(),
            entity.getSummary(),
            entity.getVersion()
        );
    }
}