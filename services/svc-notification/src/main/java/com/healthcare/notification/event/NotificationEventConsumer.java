package com.healthcare.notification.event;

import com.healthcare.platform.common.event.MessageEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationEventConsumer.class);

    public void handle(MessageEnvelope<NotificationSentEvent> envelope) {
        LOGGER.info("Consumed eventName={} correlationId={}", envelope.eventName(), envelope.correlationId());
    }
}
