package com.healthcare.notification.repository;

import com.healthcare.notification.domain.NotificationJob;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class NotificationJpaRepositoryAdapter implements NotificationRepository {
    private final JpaNotificationEntityRepository jpaRepository;

    public NotificationJpaRepositoryAdapter(JpaNotificationEntityRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public NotificationJob save(NotificationJob aggregate) {
        NotificationJobEntity saved = jpaRepository.save(toEntity(aggregate));
        return toDomain(saved);
    }

    @Override
    public Optional<NotificationJob> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<NotificationJob> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    private NotificationJobEntity toEntity(NotificationJob aggregate) {
        return new NotificationJobEntity(
                aggregate.id(),
                aggregate.status(),
                aggregate.recipientId(),
                aggregate.channel(),
                aggregate.templateId(),
            aggregate.message(),
            aggregate.deliveryAttempts(),
            aggregate.lastError()
        );
    }

    private NotificationJob toDomain(NotificationJobEntity entity) {
        return new NotificationJob(
                entity.getId(),
                entity.getStatus(),
                entity.getRecipientId(),
                entity.getChannel(),
                entity.getTemplateId(),
            entity.getMessage(),
            entity.getDeliveryAttempts(),
            entity.getLastError()
        );
    }
}