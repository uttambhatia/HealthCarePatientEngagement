package com.healthcare.careplan.integration;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.careplan.event.CarePlanCreatedEvent;
import com.healthcare.careplan.event.CarePlanEventConsumer;
import com.healthcare.careplan.event.TeleconsultationCompletedEvent;
import com.healthcare.careplan.event.TeleconsultationCompletedEventConsumer;
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
public class CarePlanQueueProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarePlanQueueProcessor.class);
    private final ServiceBusClientBuilder serviceBusClientBuilder;
    private final CarePlanEventConsumer carePlanEventConsumer;
    private final TeleconsultationCompletedEventConsumer teleconsultationCompletedEventConsumer;
    private final ObjectMapper objectMapper;
    private final String queueName;
    private final String deadLetterQueue;
    private final boolean processorEnabled;
    private final String serviceBusFqdn;
    private ServiceBusProcessorClient processorClient;
    private ServiceBusSenderClient deadLetterSender;

    public CarePlanQueueProcessor(
            ObjectProvider<ServiceBusClientBuilder> serviceBusClientBuilderProvider,
            CarePlanEventConsumer carePlanEventConsumer,
            TeleconsultationCompletedEventConsumer teleconsultationCompletedEventConsumer,
            ObjectMapper objectMapper,
            @Value("${platform.messaging.channel:careplan-service}") String queueName,
            @Value("${platform.messaging.deadLetterQueue:careplan-service-dlq}") String deadLetterQueue,
            @Value("${platform.messaging.processorEnabled:true}") boolean processorEnabled,
            @Value("${platform.azure.servicebus.fqdn:}") String serviceBusFqdn) {
        this.serviceBusClientBuilder = serviceBusClientBuilderProvider.getIfAvailable();
        this.carePlanEventConsumer = carePlanEventConsumer;
        this.teleconsultationCompletedEventConsumer = teleconsultationCompletedEventConsumer;
        this.objectMapper = objectMapper;
        this.queueName = queueName;
        this.deadLetterQueue = deadLetterQueue;
        this.processorEnabled = processorEnabled;
        this.serviceBusFqdn = serviceBusFqdn;
    }

    @PostConstruct
    public void start() {
        if (!processorEnabled) {
            LOGGER.info("Careplan queue processor is disabled by config");
            return;
        }
        if (serviceBusClientBuilder == null || serviceBusFqdn.isBlank()) {
            LOGGER.warn("Careplan queue processor not started because Service Bus is not configured");
            return;
        }
        deadLetterSender = serviceBusClientBuilder.sender().queueName(deadLetterQueue).buildClient();
        processorClient = serviceBusClientBuilder.processor().queueName(queueName).disableAutoComplete()
                .processMessage(this::processMessage).processError(this::processError).buildProcessorClient();
        processorClient.start();
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
            String eventName = text(body, "eventType", "CarePlanCreatedEvent");
            String correlationId = text(body, "correlationId", "n/a");

            if ("TeleconsultationCompletedEvent".equals(eventName)) {
                TeleconsultationCompletedEvent payload = new TeleconsultationCompletedEvent(
                        text(body, "aggregateId", ""),
                        text(body, "appointmentId", ""),
                        text(body, "patientId", ""),
                        text(body, "providerId", ""),
                        text(body, "completedAt", ""),
                        body.path("followUpRequired").asBoolean(false),
                        text(body, "nextFollowUpDate", null)
                );
                MessageEnvelope<TeleconsultationCompletedEvent> envelope =
                        new MessageEnvelope<>(correlationId, eventName, OffsetDateTime.now(), payload);
                teleconsultationCompletedEventConsumer.handle(envelope);
                context.complete();
                return;
            }

            MessageEnvelope<CarePlanCreatedEvent> envelope = new MessageEnvelope<>(
                    correlationId,
                    eventName,
                    OffsetDateTime.now(),
                    new CarePlanCreatedEvent(
                            text(body, "aggregateId", ""),
                            text(body, "patientId", ""),
                            text(body, "goal", ""),
                            text(body, "planStatus", ""),
                            text(body, "ownerId", ""),
                            java.util.List.of(),
                            intValue(body, "version", 1)));
            carePlanEventConsumer.handle(envelope);
            context.complete();
        } catch (Exception ex) {
            deadLetter(context, rawBody, ex);
        }
    }

    private void processError(ServiceBusErrorContext errorContext) {
        LOGGER.error("Careplan processor error entity={} source={} error={}", errorContext.getEntityPath(), errorContext.getErrorSource(), errorContext.getException().getMessage(), errorContext.getException());
    }

    private void deadLetter(ServiceBusReceivedMessageContext context, String rawBody, Exception ex) {
        try {
            if (deadLetterSender != null) {
                deadLetterSender.sendMessage(new ServiceBusMessage(rawBody));
            }
            context.complete();
        } catch (RuntimeException dlqError) {
            LOGGER.error("Careplan dead-letter forwarding failed error={}", dlqError.getMessage(), dlqError);
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

    private int intValue(JsonNode node, String field, int fallback) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return fallback;
        }
        try {
            return value.asInt(fallback);
        } catch (RuntimeException ex) {
            return fallback;
        }
    }
}
