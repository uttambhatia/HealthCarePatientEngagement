package com.healthcare.notification.integration;

import com.healthcare.notification.domain.NotificationJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AcsAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcsAdapter.class);

    public void dispatchNotification(NotificationJob aggregate, String correlationId) {
        LOGGER.info("External integration invoked aggregateId={} correlationId={}", aggregate.id(), correlationId);
    }
}
