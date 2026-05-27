package com.healthcare.eventmessaging.integration;

import com.healthcare.eventmessaging.domain.ServiceBusMessageRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ServiceBusAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceBusAdapter.class);

    public void queueOnBus(ServiceBusMessageRecord aggregate, String correlationId) {
        LOGGER.info("External integration invoked aggregateId={} correlationId={}", aggregate.id(), correlationId);
    }
}
