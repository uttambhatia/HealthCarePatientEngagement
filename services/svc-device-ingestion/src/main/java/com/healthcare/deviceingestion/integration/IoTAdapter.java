package com.healthcare.deviceingestion.integration;

import com.healthcare.deviceingestion.domain.DeviceMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class IoTAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(IoTAdapter.class);

    public void ingest(DeviceMessage aggregate, String correlationId) {
        LOGGER.info("External integration invoked aggregateId={} correlationId={}", aggregate.id(), correlationId);
    }
}
