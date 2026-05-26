package com.healthcare.medicalrecord.integration;

import com.healthcare.medicalrecord.domain.MedicalRecordSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FhirAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(FhirAdapter.class);

    public void upsertFhirResource(MedicalRecordSnapshot aggregate, String correlationId) {
        LOGGER.info("External integration invoked aggregateId={} correlationId={}", aggregate.id(), correlationId);
    }
}
