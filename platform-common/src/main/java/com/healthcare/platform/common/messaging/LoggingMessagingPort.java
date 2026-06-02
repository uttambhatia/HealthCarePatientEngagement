package com.healthcare.platform.common.messaging;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.healthcare.platform.common.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoggingMessagingPort implements MessagingPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingMessagingPort.class);
    private final ObjectProvider<ServiceBusClientBuilder> serviceBusClientBuilderProvider;
    private final Map<String, ServiceBusSenderClient> senderByChannel = new ConcurrentHashMap<>();

    public LoggingMessagingPort(ObjectProvider<ServiceBusClientBuilder> serviceBusClientBuilderProvider) {
        this.serviceBusClientBuilderProvider = serviceBusClientBuilderProvider;
    }

    @Override
    public void publish(String channel, String correlationId, DomainEvent event) {
        ServiceBusClientBuilder builder = serviceBusClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            logFallback(channel, correlationId, event, "ServiceBusClientBuilder bean is unavailable");
            return;
        }

        ServiceBusSenderClient sender = senderByChannel.computeIfAbsent(channel, targetChannel -> {
            try {
                return builder.sender().queueName(targetChannel).buildClient();
            } catch (RuntimeException ex) {
                LOGGER.warn("Service Bus sender initialization failed channel={} error={}", targetChannel, ex.getMessage());
                return null;
            }
        });

        if (sender == null) {
            logFallback(channel, correlationId, event, "Service Bus sender is not initialized");
            return;
        }

        try {
            sender.sendMessage(new ServiceBusMessage(toJson(channel, correlationId, event)));
            LOGGER.info("Published eventType={} aggregateId={} channel={} correlationId={} via=servicebus",
                    event.eventType(), event.aggregateId(), channel, correlationId);
        } catch (RuntimeException ex) {
            logFallback(channel, correlationId, event, "Service Bus publish failed: " + ex.getMessage());
        }
    }

    private void logFallback(String channel, String correlationId, DomainEvent event, String reason) {
        LOGGER.warn("Publishing fallback to logs reason={} eventType={} aggregateId={} channel={} correlationId={}",
                reason, event.eventType(), event.aggregateId(), channel, correlationId);
    }

    private String toJson(String channel, String correlationId, DomainEvent event) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("eventType", event.eventType());
        fields.put("aggregateId", event.aggregateId());
        fields.put("channel", channel);
        fields.put("correlationId", correlationId);

        // Include event-specific record fields so downstream processors can apply business rules.
        RecordComponent[] recordComponents = event.getClass().getRecordComponents();
        if (recordComponents != null) {
            for (RecordComponent component : recordComponents) {
                String name = component.getName();
                if (fields.containsKey(name)) {
                    continue;
                }
                try {
                    Method accessor = component.getAccessor();
                    Object value = accessor.invoke(event);
                    fields.put(name, value == null ? "" : String.valueOf(value));
                } catch (Exception ex) {
                    LOGGER.debug("Unable to serialize event field field={} eventType={} error={}",
                            name, event.eventType(), ex.getMessage());
                }
            }
        }

        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (!first) {
                json.append(',');
            }
            json.append("\"").append(escape(entry.getKey())).append("\":\"")
                    .append(escape(entry.getValue())).append("\"");
            first = false;
        }
        json.append('}');
        return json.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
