package com.healthcare.alertmanagement.integration;

import com.healthcare.alertmanagement.domain.ClinicalAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AlertEscalationAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlertEscalationAdapter.class);

    public void escalateAlert(ClinicalAlert aggregate, String correlationId) {
        LOGGER.info("External integration invoked aggregateId={} correlationId={}", aggregate.id(), correlationId);
    }
}
