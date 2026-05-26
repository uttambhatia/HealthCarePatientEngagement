package com.healthcare.eventmessaging.service;

import com.healthcare.eventmessaging.domain.ServiceBusMessageRecord;
import com.healthcare.eventmessaging.dto.CreateServiceBusMessageRequest;
import com.healthcare.eventmessaging.dto.ServiceBusMessageResponse;
import com.healthcare.eventmessaging.event.ServiceBusMessageQueuedEvent;
import com.healthcare.eventmessaging.exception.ResourceNotFoundException;
import com.healthcare.eventmessaging.integration.ServiceBusAdapter;
import com.healthcare.eventmessaging.repository.ServiceBusMessageRepository;
import com.healthcare.platform.common.messaging.MessagingPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EventMessagingApplicationServiceImpl implements EventMessagingApplicationService {
    private final ServiceBusMessageRepository repository;
    private final MessagingPort messagingPort;
    private final ServiceBusAdapter integration;

    public EventMessagingApplicationServiceImpl(ServiceBusMessageRepository repository, MessagingPort messagingPort, ServiceBusAdapter integration) {
        this.repository = repository;
        this.messagingPort = messagingPort;
        this.integration = integration;
    }

    @Override
    public ServiceBusMessageResponse queueMessage(CreateServiceBusMessageRequest request, String correlationId) {
        ServiceBusMessageRecord aggregate = repository.save(new ServiceBusMessageRecord(
                UUID.randomUUID().toString(),
                "QUEUED",
                request.channel(),
        request.eventName(),
        request.payload(),
        request.messageType()
        ));
        integration.queueOnBus(aggregate, correlationId);
        messagingPort.publish("event-messaging-service", correlationId, new ServiceBusMessageQueuedEvent(
                aggregate.id(),
                aggregate.channel(),
                aggregate.eventName(),
                aggregate.payload(),
                aggregate.messageType()
        ));
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
        aggregate.messageType()
        );
    }
}
