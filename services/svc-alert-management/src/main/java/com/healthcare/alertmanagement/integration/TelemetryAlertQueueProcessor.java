package com.healthcare.alertmanagement.integration;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.alertmanagement.event.TelemetryAlertEventConsumer;
import com.healthcare.alertmanagement.event.TelemetryReceivedEvent;
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
public class TelemetryAlertQueueProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryAlertQueueProcessor.class);

    private final ServiceBusClientBuilder serviceBusClientBuilder;
    private final TelemetryAlertEventConsumer telemetryAlertEventConsumer;
    private final ObjectMapper objectMapper;
    private final String queueName;
    private final String deadLetterQueue;
    private final boolean processorEnabled;
    private final String serviceBusFqdn;

    private ServiceBusProcessorClient processorClient;
    private ServiceBusSenderClient deadLetterSender;

    public TelemetryAlertQueueProcessor(
            ObjectProvider<ServiceBusClientBuilder> serviceBusClientBuilderProvider,
            TelemetryAlertEventConsumer telemetryAlertEventConsumer,
            ObjectMapper objectMapper,
            @Value("${platform.integration.telemetry.alert-source-channel:telemetry-service}") String queueName,
            @Value("${platform.messaging.deadLetterQueue:alert-management-service-dlq}") String deadLetterQueue,
            @Value("${platform.messaging.processorEnabled:true}") boolean processorEnabled,
            @Value("${platform.azure.servicebus.fqdn:}") String serviceBusFqdn) {
        this.serviceBusClientBuilder = serviceBusClientBuilderProvider.getIfAvailable();
        this.telemetryAlertEventConsumer = telemetryAlertEventConsumer;
        this.objectMapper = objectMapper;
        this.queueName = queueName;
        this.deadLetterQueue = deadLetterQueue;
        this.processorEnabled = processorEnabled;
        this.serviceBusFqdn = serviceBusFqdn;
    }

    @PostConstruct
    public void start() {
        if (!processorEnabled) {
            LOGGER.info("Telemetry alert queue processor is disabled by config");
            return;
        }
        if (serviceBusClientBuilder == null || serviceBusFqdn.isBlank()) {
            LOGGER.warn("Telemetry alert queue processor not started because Service Bus is not configured");
            return;
        }

        deadLetterSender = serviceBusClientBuilder.sender().queueName(deadLetterQueue).buildClient();
        processorClient = serviceBusClientBuilder.processor()
                .queueName(queueName)
                .disableAutoComplete()
                .processMessage(this::processMessage)
                .processError(this::processError)
                .buildProcessorClient();
        processorClient.start();
        LOGGER.info("Telemetry alert queue processor started queue={} deadLetterQueue={}", queueName, deadLetterQueue);
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
            String eventName = text(body, "eventType", "");
            if (!"TelemetryReceivedEvent".equals(eventName)) {
                context.complete();
                return;
            }

            String correlationId = text(body, "correlationId", "n/a");
            TelemetryReceivedEvent payload = new TelemetryReceivedEvent(
                    text(body, "aggregateId", ""),
                    text(body, "deviceId", ""),
                    text(body, "metricType", ""),
                    text(body, "metricValue", ""),
                    text(body, "recordedAt", "")
            );

            MessageEnvelope<TelemetryReceivedEvent> envelope =
                    new MessageEnvelope<>(correlationId, eventName, OffsetDateTime.now(), payload);

            telemetryAlertEventConsumer.handle(envelope);
            context.complete();
        } catch (Exception ex) {
            deadLetter(context, rawBody, ex);
        }
    }

    private void processError(ServiceBusErrorContext errorContext) {
        LOGGER.error("Telemetry alert processor error entity={} source={} error={}",
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
            LOGGER.warn("Telemetry alert message forwarded to dead-letter queue={} reason={}", deadLetterQueue, ex.getMessage());
        } catch (RuntimeException dlqError) {
            LOGGER.error("Telemetry alert dead-letter forwarding failed; abandoning message error={}",
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
}
