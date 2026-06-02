package com.healthcare.deviceingestion.repository;

import com.healthcare.deviceingestion.domain.DeviceMessage;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class DeviceIngestionJpaRepositoryAdapter implements DeviceIngestionRepository {
    private final JpaDeviceMessageEntityRepository jpaRepository;

    public DeviceIngestionJpaRepositoryAdapter(JpaDeviceMessageEntityRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public DeviceMessage save(DeviceMessage aggregate) {
        DeviceMessageEntity saved = jpaRepository.save(toEntity(aggregate));
        return toDomain(saved);
    }

    @Override
    public Optional<DeviceMessage> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<DeviceMessage> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    private DeviceMessageEntity toEntity(DeviceMessage aggregate) {
        return new DeviceMessageEntity(
                aggregate.id(),
                aggregate.status(),
                aggregate.deviceId(),
                aggregate.protocol(),
                aggregate.payload(),
                aggregate.receivedAt()
        );
    }

    private DeviceMessage toDomain(DeviceMessageEntity entity) {
        return new DeviceMessage(
                entity.getId(),
                entity.getStatus(),
                entity.getDeviceId(),
                entity.getProtocol(),
                entity.getPayload(),
                entity.getReceivedAt()
        );
    }
}
