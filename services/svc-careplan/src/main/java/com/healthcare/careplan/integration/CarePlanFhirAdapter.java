package com.healthcare.careplan.integration;

import com.healthcare.careplan.domain.CarePlanAggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CarePlanFhirAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarePlanFhirAdapter.class);

    public void synchronizeCarePlan(CarePlanAggregate aggregate, String correlationId) {
        LOGGER.info("External integration invoked aggregateId={} correlationId={}", aggregate.id(), correlationId);
    }
}
