package com.healthcare.alertmanagement.repository;

import com.healthcare.alertmanagement.domain.ClinicalAlert;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class AlertJpaRepositoryAdapter implements AlertRepository {
    private final JpaClinicalAlertEntityRepository jpaRepository;

    public AlertJpaRepositoryAdapter(JpaClinicalAlertEntityRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ClinicalAlert save(ClinicalAlert aggregate) {
        ClinicalAlertEntity saved = jpaRepository.save(toEntity(aggregate));
        return toDomain(saved);
    }

    @Override
    public Optional<ClinicalAlert> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<ClinicalAlert> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    private ClinicalAlertEntity toEntity(ClinicalAlert aggregate) {
        return new ClinicalAlertEntity(
                aggregate.id(),
                aggregate.status(),
                aggregate.patientId(),
            aggregate.deviceId(),
                aggregate.severity(),
                aggregate.triggerType(),
                aggregate.summary()
        );
    }

    private ClinicalAlert toDomain(ClinicalAlertEntity entity) {
        return new ClinicalAlert(
                entity.getId(),
                entity.getStatus(),
                entity.getPatientId(),
            entity.getDeviceId(),
                entity.getSeverity(),
                entity.getTriggerType(),
                entity.getSummary()
        );
    }
}
