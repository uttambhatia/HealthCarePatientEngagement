package com.healthcare.appointment.event;

import com.healthcare.platform.common.event.MessageEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AppointmentEventConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppointmentEventConsumer.class);

    public void handle(MessageEnvelope<AppointmentBookedEvent> envelope) {
        LOGGER.info("Consumed eventName={} correlationId={}", envelope.eventName(), envelope.correlationId());
    }
}
