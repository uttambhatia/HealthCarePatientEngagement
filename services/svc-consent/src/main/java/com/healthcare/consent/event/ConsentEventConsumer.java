package com.healthcare.consent.event;

import com.healthcare.platform.common.event.MessageEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ConsentEventConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsentEventConsumer.class);

    public void handle(MessageEnvelope<ConsentUpdatedEvent> envelope) {
        LOGGER.info("Consumed eventName={} correlationId={}", envelope.eventName(), envelope.correlationId());
    }
}
