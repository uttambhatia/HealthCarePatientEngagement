package com.healthcare.appointment.integration;

import com.healthcare.appointment.domain.AppointmentRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AppointmentNotificationAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppointmentNotificationAdapter.class);

    public void sendBookingNotification(AppointmentRecord aggregate, String correlationId) {
        LOGGER.info("External integration invoked aggregateId={} correlationId={}", aggregate.id(), correlationId);
    }
}
