package com.healthcare.consent.integration;

import com.healthcare.consent.domain.ConsentRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ConsentAuditAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsentAuditAdapter.class);

    public void publishAuditTrail(ConsentRecord aggregate, String correlationId) {
        LOGGER.info("External integration invoked aggregateId={} correlationId={}", aggregate.id(), correlationId);
    }
}
