package com.healthcare.deviceingestion.event;

import com.healthcare.platform.common.event.MessageEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DeviceIngestionEventConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceIngestionEventConsumer.class);

    public void handle(MessageEnvelope<DeviceSignalIngestedEvent> envelope) {
        LOGGER.info("Consumed eventName={} correlationId={}", envelope.eventName(), envelope.correlationId());
    }
}
