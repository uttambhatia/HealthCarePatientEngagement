package com.healthcare.platform.common.messaging;

import com.healthcare.platform.common.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingMessagingPort implements MessagingPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingMessagingPort.class);

    @Override
    public void publish(String channel, String correlationId, DomainEvent event) {
        LOGGER.info("Publishing eventType={} aggregateId={} channel={} correlationId={}", event.eventType(), event.aggregateId(), channel, correlationId);
    }
}
