package com.healthcare.notification.integration;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.notification.event.NotificationEventConsumer;
import com.healthcare.notification.event.TeleconsultationCompletedEvent;
import com.healthcare.platform.common.event.MessageEnvelope;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class TeleconsultationCompletedTopicProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeleconsultationCompletedTopicProcessor.class);

    private final ServiceBusClientBuilder serviceBusClientBuilder;
    private final NotificationEventConsumer notificationEventConsumer;
    private final ObjectMapper objectMapper;
    private final String topicName;
    private final String subscriptionName;
    private final String deadLetterQueue;
    private final boolean processorEnabled;
    private final String serviceBusFqdn;

    private ServiceBusProcessorClient processorClient;
    private ServiceBusSenderClient deadLetterSender;

    public TeleconsultationCompletedTopicProcessor(
            ObjectProvider<ServiceBusClientBuilder> serviceBusClientBuilderProvider,
            NotificationEventConsumer notificationEventConsumer,
            ObjectMapper objectMapper,
            @Value("${platform.messaging.teleconsultation.completedTopic:teleconsultation-completed}") String topicName,
            @Value("${platform.messaging.teleconsultation.completedSubscription:notification-service}") String subscriptionName,
            @Value("${platform.messaging.deadLetterQueue:notification-service-dlq}") String deadLetterQueue,
            @Value("${platform.messaging.teleconsultation.processorEnabled:true}") boolean processorEnabled,
            @Value("${platform.azure.servicebus.fqdn:}") String serviceBusFqdn) {
        this.serviceBusClientBuilder = serviceBusClientBuilderProvider.getIfAvailable();
        this.notificationEventConsumer = notificationEventConsumer;
        this.objectMapper = objectMapper;
        this.topicName = topicName;
        this.subscriptionName = subscriptionName;
        this.deadLetterQueue = deadLetterQueue;
        this.processorEnabled = processorEnabled;
        this.serviceBusFqdn = serviceBusFqdn;
    }

    @PostConstruct
    public void start() {
        if (!processorEnabled) {
            LOGGER.info("Teleconsultation topic processor is disabled by config");
            return;
        }
        if (serviceBusClientBuilder == null || serviceBusFqdn.isBlank()) {
            LOGGER.warn("Teleconsultation topic processor not started because Service Bus is not configured");
            return;
        }

        deadLetterSender = serviceBusClientBuilder.sender().queueName(deadLetterQueue).buildClient();
        processorClient = serviceBusClientBuilder.processor()
                .topicName(topicName)
                .subscriptionName(subscriptionName)
                .disableAutoComplete()
                .processMessage(this::processMessage)
                .processError(this::processError)
                .buildProcessorClient();
        processorClient.start();
        LOGGER.info("Teleconsultation topic processor started topic={} subscription={} deadLetterQueue={}",
                topicName, subscriptionName, deadLetterQueue);
    }

    @PreDestroy
    public void stop() {
        if (processorClient != null) {
            processorClient.close();
            processorClient = null;
        }
        if (deadLetterSender != null) {
            deadLetterSender.close();
            deadLetterSender = null;
        }
    }

    private void processMessage(ServiceBusReceivedMessageContext context) {
        String rawBody = context.getMessage().getBody().toString();
        try {
            JsonNode body = objectMapper.readTree(rawBody);
            String eventName = text(body, "eventType", "UnknownEvent");
            if (!"TeleconsultationCompletedEvent".equals(eventName)) {
                context.complete();
                return;
            }

            String correlationId = text(body, "correlationId", "n/a");
            TeleconsultationCompletedEvent payload = new TeleconsultationCompletedEvent(
                    text(body, "aggregateId", ""),
                    text(body, "appointmentId", ""),
                    text(body, "patientId", ""),
                    text(body, "providerId", ""),
                    text(body, "completedAt", ""),
                    bool(body, "followUpRequired", false),
                    text(body, "nextFollowUpDate", "")
            );

            MessageEnvelope<TeleconsultationCompletedEvent> envelope =
                    new MessageEnvelope<>(correlationId, eventName, OffsetDateTime.now(), payload);
            notificationEventConsumer.handleTeleconsultationCompleted(envelope);
            context.complete();
        } catch (Exception ex) {
            deadLetter(context, rawBody, ex);
        }
    }

    private void processError(ServiceBusErrorContext errorContext) {
        LOGGER.error("Teleconsultation topic processor error entity={} source={} error={}",
                errorContext.getEntityPath(),
                errorContext.getErrorSource(),
                errorContext.getException().getMessage(),
                errorContext.getException());
    }

    private void deadLetter(ServiceBusReceivedMessageContext context, String rawBody, Exception ex) {
        try {
            if (deadLetterSender != null) {
                deadLetterSender.sendMessage(new ServiceBusMessage(rawBody));
            }
            context.complete();
            LOGGER.warn("Teleconsultation topic message forwarded to dead-letter queue={} reason={}",
                    deadLetterQueue, ex.getMessage());
        } catch (RuntimeException dlqError) {
            LOGGER.error("Teleconsultation topic dead-letter forwarding failed; abandoning message error={}",
                    dlqError.getMessage(), dlqError);
            context.abandon();
        }
    }

    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            return fallback;
        }
        return value.asText();
    }

    private boolean bool(JsonNode node, String field, boolean fallback) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return fallback;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        String text = value.asText();
        if (text == null || text.isBlank()) {
            return fallback;
        }
        return Boolean.parseBoolean(text);
    }
}
