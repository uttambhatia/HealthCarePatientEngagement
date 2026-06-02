package com.healthcare.medicalrecord.integration;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.medicalrecord.event.MedicalRecordEventConsumer;
import com.healthcare.medicalrecord.event.MedicalRecordSynchronizedEvent;
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
public class MedicalRecordQueueProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MedicalRecordQueueProcessor.class);
    private final ServiceBusClientBuilder serviceBusClientBuilder;
    private final MedicalRecordEventConsumer medicalRecordEventConsumer;
    private final ObjectMapper objectMapper;
    private final String queueName;
    private final String deadLetterQueue;
    private final boolean processorEnabled;
    private final String serviceBusFqdn;
    private ServiceBusProcessorClient processorClient;
    private ServiceBusSenderClient deadLetterSender;

    public MedicalRecordQueueProcessor(
            ObjectProvider<ServiceBusClientBuilder> serviceBusClientBuilderProvider,
            MedicalRecordEventConsumer medicalRecordEventConsumer,
            ObjectMapper objectMapper,
            @Value("${platform.messaging.channel:medical-record-service}") String queueName,
            @Value("${platform.messaging.deadLetterQueue:medical-record-service-dlq}") String deadLetterQueue,
            @Value("${platform.messaging.processorEnabled:true}") boolean processorEnabled,
            @Value("${platform.azure.servicebus.fqdn:}") String serviceBusFqdn) {
        this.serviceBusClientBuilder = serviceBusClientBuilderProvider.getIfAvailable();
        this.medicalRecordEventConsumer = medicalRecordEventConsumer;
        this.objectMapper = objectMapper;
        this.queueName = queueName;
        this.deadLetterQueue = deadLetterQueue;
        this.processorEnabled = processorEnabled;
        this.serviceBusFqdn = serviceBusFqdn;
    }

    @PostConstruct
    public void start() {
        if (!processorEnabled || serviceBusClientBuilder == null || serviceBusFqdn.isBlank()) {
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
            MessageEnvelope<MedicalRecordSynchronizedEvent> envelope = new MessageEnvelope<>(
                    text(body, "correlationId", "n/a"),
                    text(body, "eventType", "MedicalRecordSynchronizedEvent"),
                    OffsetDateTime.now(),
                    new MedicalRecordSynchronizedEvent(
                            text(body, "aggregateId", ""),
                            text(body, "patientId", ""),
                            text(body, "fhirResourceType", ""),
                            text(body, "resourceReference", ""),
                            text(body, "summary", "")));
            medicalRecordEventConsumer.handle(envelope);
            context.complete();
        } catch (Exception ex) {
            deadLetter(context, rawBody);
        }
    }

    private void processError(ServiceBusErrorContext errorContext) {
        LOGGER.error("Medical-record processor error: {}", errorContext.getException().getMessage(), errorContext.getException());
    }

    private void deadLetter(ServiceBusReceivedMessageContext context, String rawBody) {
        try {
            if (deadLetterSender != null) {
                deadLetterSender.sendMessage(new ServiceBusMessage(rawBody));
            }
            context.complete();
        } catch (RuntimeException dlqError) {
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
}
