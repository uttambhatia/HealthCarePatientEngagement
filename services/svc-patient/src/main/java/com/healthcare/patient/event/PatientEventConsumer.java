package com.healthcare.patient.event;

import com.healthcare.platform.common.event.MessageEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PatientEventConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PatientEventConsumer.class);

    public void handle(MessageEnvelope<PatientRegisteredEvent> envelope) {
        LOGGER.info("Consumed eventName={} correlationId={}", envelope.eventName(), envelope.correlationId());
    }
}
