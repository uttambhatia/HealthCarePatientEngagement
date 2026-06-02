package com.healthcare.notification.service;

import com.healthcare.notification.domain.NotificationJob;
import com.healthcare.notification.dto.CreateNotificationRequest;
import com.healthcare.notification.dto.NotificationResponse;
import com.healthcare.notification.event.NotificationSentEvent;
import com.healthcare.notification.exception.NotificationDeliveryFailedException;
import com.healthcare.notification.exception.ResourceNotFoundException;
import com.healthcare.notification.exception.UnsupportedNotificationChannelException;
import com.healthcare.notification.integration.AcsAdapter;
import com.healthcare.notification.repository.NotificationRepository;
import com.healthcare.platform.common.messaging.MessagingPort;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class NotificationApplicationServiceImpl implements NotificationApplicationService {
    private static final Set<String> SUPPORTED_CHANNELS = new LinkedHashSet<>(List.of("SMS", "EMAIL", "PUSH"));

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
        String normalizedChannel = normalizeChannel(request.channel());
        validateChannel(normalizedChannel);

        NotificationJob queued = repository.save(new NotificationJob(
                UUID.randomUUID().toString(),
                "QUEUED",
                request.recipientId(),
            normalizedChannel,
            request.templateId(),
            request.message(),
            0,
            null
        ));

        NotificationJob delivered;
        try {
            int attempts = integration.dispatchNotification(queued, correlationId);
            delivered = repository.save(new NotificationJob(
                queued.id(),
                "DELIVERED",
                queued.recipientId(),
                queued.channel(),
                queued.templateId(),
                queued.message(),
                attempts,
                null
            ));
        } catch (RuntimeException ex) {
            NotificationJob failed = repository.save(new NotificationJob(
                queued.id(),
                "FAILED",
                queued.recipientId(),
                queued.channel(),
                queued.templateId(),
                queued.message(),
                integration.maxAttempts(),
                sanitizeError(ex.getMessage())
            ));
            throw new NotificationDeliveryFailedException(failed.id(), failed.lastError(), ex);
        }

        messagingPort.publish("notification-service", correlationId, new NotificationSentEvent(
            delivered.id(),
            delivered.recipientId(),
            delivered.channel(),
            delivered.templateId(),
            delivered.message()
        ));
        return map(delivered);
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
                aggregate.message(),
                aggregate.deliveryAttempts(),
                aggregate.lastError()
        );
    }

    private String normalizeChannel(String channel) {
        return channel == null ? "" : channel.trim().toUpperCase(Locale.ROOT);
    }

    private void validateChannel(String channel) {
        if (!SUPPORTED_CHANNELS.contains(channel)) {
            throw new UnsupportedNotificationChannelException(channel);
        }
    }

    private String sanitizeError(String message) {
        if (message == null || message.isBlank()) {
            return "Unknown delivery failure";
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
