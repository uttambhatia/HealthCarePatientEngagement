package com.healthcare.eventmessaging.repository;

import com.healthcare.eventmessaging.domain.ServiceBusMessageRecord;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class ServiceBusMessageJpaRepositoryAdapter implements ServiceBusMessageRepository {
    private final JpaServiceBusMessageEntityRepository jpaRepository;

    public ServiceBusMessageJpaRepositoryAdapter(JpaServiceBusMessageEntityRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ServiceBusMessageRecord save(ServiceBusMessageRecord aggregate) {
        ServiceBusMessageEntity saved = jpaRepository.save(toEntity(aggregate));
        return toDomain(saved);
    }

    @Override
    public Optional<ServiceBusMessageRecord> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<ServiceBusMessageRecord> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    private ServiceBusMessageEntity toEntity(ServiceBusMessageRecord aggregate) {
        return new ServiceBusMessageEntity(
                aggregate.id(),
                aggregate.status(),
                aggregate.channel(),
                aggregate.eventName(),
                aggregate.payload(),
                aggregate.messageType(),
                aggregate.recordedAt(),
                aggregate.integrityHash(),
                aggregate.anomalyReason()
        );
    }

    private ServiceBusMessageRecord toDomain(ServiceBusMessageEntity entity) {
        return new ServiceBusMessageRecord(
                entity.getId(),
                entity.getStatus(),
                entity.getChannel(),
                entity.getEventName(),
                entity.getPayload(),
                entity.getMessageType(),
                entity.getRecordedAt(),
                entity.getIntegrityHash(),
                entity.getAnomalyReason()
        );
    }
}
