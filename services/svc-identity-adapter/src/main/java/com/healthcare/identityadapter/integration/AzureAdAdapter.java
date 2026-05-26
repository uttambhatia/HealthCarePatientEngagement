package com.healthcare.identityadapter.integration;

import com.healthcare.identityadapter.domain.IdentityAssertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AzureAdAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureAdAdapter.class);

    public void validateAccessToken(IdentityAssertion aggregate, String correlationId) {
        LOGGER.info("External integration invoked aggregateId={} correlationId={}", aggregate.id(), correlationId);
    }
}
