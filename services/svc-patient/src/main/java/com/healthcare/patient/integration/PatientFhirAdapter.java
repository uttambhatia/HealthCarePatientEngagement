package com.healthcare.patient.integration;

import com.healthcare.patient.domain.PatientProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PatientFhirAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PatientFhirAdapter.class);

    public void synchronizeProfile(PatientProfile aggregate, String correlationId) {
        LOGGER.info("External integration invoked aggregateId={} correlationId={}", aggregate.id(), correlationId);
    }
}
