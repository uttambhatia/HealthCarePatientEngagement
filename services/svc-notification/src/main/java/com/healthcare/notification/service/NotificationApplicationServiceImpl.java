package com.healthcare.notification.service;

import com.healthcare.notification.domain.NotificationJob;
import com.healthcare.notification.dto.CreateNotificationRequest;
import com.healthcare.notification.dto.NotificationResponse;
import com.healthcare.notification.event.NotificationSentEvent;
import com.healthcare.notification.exception.ResourceNotFoundException;
import com.healthcare.notification.integration.AcsAdapter;
import com.healthcare.notification.repository.NotificationRepository;
import com.healthcare.platform.common.messaging.MessagingPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationApplicationServiceImpl implements NotificationApplicationService {
    private final NotificationRepository repository;
    private final MessagingPort messagingPort;
    private final AcsAdapter integration;

    public NotificationApplicationServiceImpl(NotificationRepository repository, MessagingPort messagingPort, AcsAdapter integration) {
        this.repository = repository;
        this.messagingPort = messagingPort;
        this.integration = integration;
    }

    @Override
    public NotificationResponse sendNotification(CreateNotificationRequest request, String correlationId) {
        NotificationJob aggregate = repository.save(new NotificationJob(
                UUID.randomUUID().toString(),
                "QUEUED",
                request.recipientId(),
        request.channel(),
        request.templateId(),
        request.message()
        ));
        integration.dispatchNotification(aggregate, correlationId);
        messagingPort.publish("notification-service", correlationId, new NotificationSentEvent(
                aggregate.id(),
                aggregate.recipientId(),
                aggregate.channel(),
                aggregate.templateId(),
                aggregate.message()
        ));
        return map(aggregate);
    }

    @Override
    public NotificationResponse getNotification(String id) {
        return repository.findById(id).map(this::map)
                .orElseThrow(() -> new ResourceNotFoundException("Notification record not found: " + id));
    }

    @Override
    public List<NotificationResponse> listNotifications() {
        return repository.findAll().stream().map(this::map).toList();
    }


    private NotificationResponse map(NotificationJob aggregate) {
        return new NotificationResponse(
                aggregate.id(),
                aggregate.status(),
                aggregate.recipientId(),
        aggregate.channel(),
        aggregate.templateId(),
        aggregate.message()
        );
    }
}
