package com.healthcare.identityadapter.event;

import com.healthcare.platform.common.event.MessageEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class IdentityEventConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdentityEventConsumer.class);

    public void handle(MessageEnvelope<IdentityValidatedEvent> envelope) {
        LOGGER.info("Consumed eventName={} correlationId={}", envelope.eventName(), envelope.correlationId());
    }
}
