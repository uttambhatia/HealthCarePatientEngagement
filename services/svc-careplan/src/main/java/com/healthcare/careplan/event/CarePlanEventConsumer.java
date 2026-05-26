package com.healthcare.careplan.event;

import com.healthcare.platform.common.event.MessageEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CarePlanEventConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarePlanEventConsumer.class);

    public void handle(MessageEnvelope<CarePlanCreatedEvent> envelope) {
        LOGGER.info("Consumed eventName={} correlationId={}", envelope.eventName(), envelope.correlationId());
    }
}
