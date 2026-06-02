package com.healthcare.telemetry.repository;

import com.healthcare.telemetry.domain.TelemetryReading;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class TelemetryJpaRepositoryAdapter implements TelemetryRepository {
    private final JpaTelemetryEntityRepository jpaRepository;

    public TelemetryJpaRepositoryAdapter(JpaTelemetryEntityRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public TelemetryReading save(TelemetryReading aggregate) {
        TelemetryReadingEntity saved = jpaRepository.save(toEntity(aggregate));
        return toDomain(saved);
    }

    @Override
    public Optional<TelemetryReading> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<TelemetryReading> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<TelemetryReading> findByDeviceIds(List<String> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findByDeviceIdIn(deviceIds).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    private TelemetryReadingEntity toEntity(TelemetryReading aggregate) {
        return new TelemetryReadingEntity(
                aggregate.id(),
                aggregate.status(),
                aggregate.deviceId(),
                aggregate.metricType(),
                aggregate.metricValue(),
                aggregate.recordedAt()
        );
    }

    private TelemetryReading toDomain(TelemetryReadingEntity entity) {
        return new TelemetryReading(
                entity.getId(),
                entity.getStatus(),
                entity.getDeviceId(),
                entity.getMetricType(),
                entity.getMetricValue(),
                entity.getRecordedAt()
        );
    }
}