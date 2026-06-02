package com.healthcare.eventmessaging.service;

import com.healthcare.eventmessaging.domain.ServiceBusMessageRecord;
import com.healthcare.eventmessaging.dto.CreateServiceBusMessageRequest;
import com.healthcare.eventmessaging.dto.ServiceBusMessageResponse;
import com.healthcare.eventmessaging.event.MonitoringAnomalyDetectedEvent;
import com.healthcare.eventmessaging.event.ServiceBusMessageQueuedEvent;
import com.healthcare.eventmessaging.exception.MonitoringDispatchFailedException;
import com.healthcare.eventmessaging.exception.ResourceNotFoundException;
import com.healthcare.eventmessaging.integration.MonitoringAdapter;
import com.healthcare.eventmessaging.integration.ServiceBusAdapter;
import com.healthcare.eventmessaging.repository.ServiceBusMessageRepository;
import com.healthcare.platform.common.messaging.MessagingPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class EventMessagingApplicationServiceImpl implements EventMessagingApplicationService {
    private final ServiceBusMessageRepository repository;
    private final MessagingPort messagingPort;
    private final ServiceBusAdapter integration;
    private final MonitoringAdapter monitoringAdapter;
    private final int anomalyPayloadMaxLength;
    private final List<String> anomalyKeywords;

    public EventMessagingApplicationServiceImpl(
            ServiceBusMessageRepository repository,
            MessagingPort messagingPort,
            ServiceBusAdapter integration,
            MonitoringAdapter monitoringAdapter,
            @Value("${platform.monitoring.anomaly.payload-max-length:500}") int anomalyPayloadMaxLength,
            @Value("${platform.monitoring.anomaly.keywords:ERROR,EXCEPTION,PANIC}") String anomalyKeywords) {
        this.repository = repository;
        this.messagingPort = messagingPort;
        this.integration = integration;
        this.monitoringAdapter = monitoringAdapter;
        this.anomalyPayloadMaxLength = Math.max(1, anomalyPayloadMaxLength);
        this.anomalyKeywords = Arrays.stream(anomalyKeywords.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> s.toUpperCase(Locale.ROOT))
                .toList();
    }

    @Override
    public ServiceBusMessageResponse queueMessage(CreateServiceBusMessageRequest request, String correlationId) {
        String anomalyReason = detectAnomalyReason(request);
        String recordedAt = OffsetDateTime.now().toString();
        String integrityHash = hashRecord(request, recordedAt);

        ServiceBusMessageRecord aggregate = repository.save(new ServiceBusMessageRecord(
                UUID.randomUUID().toString(),
                anomalyReason == null ? "QUEUED" : "ANOMALY_DETECTED",
                request.channel(),
                request.eventName(),
                request.payload(),
                request.messageType(),
                recordedAt,
                integrityHash,
                anomalyReason
        ));

        integration.queueOnBus(aggregate, correlationId);

        try {
            monitoringAdapter.publishAuditRecord(aggregate, correlationId);
        } catch (RuntimeException ex) {
            throw new MonitoringDispatchFailedException(aggregate.id(), ex);
        }

        messagingPort.publish("event-messaging-service", correlationId, new ServiceBusMessageQueuedEvent(
                aggregate.id(),
                aggregate.channel(),
                aggregate.eventName(),
                aggregate.payload(),
                aggregate.messageType()
        ));

        if (aggregate.anomalyReason() != null) {
            messagingPort.publish("event-messaging-service", correlationId, new MonitoringAnomalyDetectedEvent(
                    aggregate.id(),
                    aggregate.channel(),
                    aggregate.eventName(),
                    aggregate.messageType(),
                    aggregate.anomalyReason()
            ));
        }

        return map(aggregate);
    }

    @Override
    public ServiceBusMessageResponse getMessage(String id) {
        return repository.findById(id).map(this::map)
                .orElseThrow(() -> new ResourceNotFoundException("EventMessaging record not found: " + id));
    }

    @Override
    public List<ServiceBusMessageResponse> listMessages() {
        return repository.findAll().stream().map(this::map).toList();
    }


    private ServiceBusMessageResponse map(ServiceBusMessageRecord aggregate) {
        return new ServiceBusMessageResponse(
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

    private String detectAnomalyReason(CreateServiceBusMessageRequest request) {
        String payload = request.payload() == null ? "" : request.payload();
        if (payload.length() > anomalyPayloadMaxLength) {
            return "PAYLOAD_LENGTH_EXCEEDED";
        }

        String payloadUpper = payload.toUpperCase(Locale.ROOT);
        for (String keyword : anomalyKeywords) {
            if (payloadUpper.contains(keyword)) {
                return "KEYWORD_MATCH:" + keyword;
            }
        }
        return null;
    }

    private String hashRecord(CreateServiceBusMessageRequest request, String recordedAt) {
        String canonical = request.channel() + "|" + request.eventName() + "|" + request.payload() + "|"
                + request.messageType() + "|" + recordedAt;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : encoded) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
