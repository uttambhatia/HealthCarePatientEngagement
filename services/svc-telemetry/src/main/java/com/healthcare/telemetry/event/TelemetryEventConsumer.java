package com.healthcare.telemetry.event;

import com.healthcare.platform.common.event.MessageEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TelemetryEventConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryEventConsumer.class);

    public void handle(MessageEnvelope<TelemetryReceivedEvent> envelope) {
        LOGGER.info("Consumed eventName={} correlationId={}", envelope.eventName(), envelope.correlationId());
    }
}
