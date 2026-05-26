package com.healthcare.medicalrecord.event;

import com.healthcare.platform.common.event.MessageEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MedicalRecordEventConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MedicalRecordEventConsumer.class);

    public void handle(MessageEnvelope<MedicalRecordSynchronizedEvent> envelope) {
        LOGGER.info("Consumed eventName={} correlationId={}", envelope.eventName(), envelope.correlationId());
    }
}
