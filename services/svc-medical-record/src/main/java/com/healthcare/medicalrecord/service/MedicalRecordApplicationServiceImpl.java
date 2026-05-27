package com.healthcare.medicalrecord.service;

import com.healthcare.medicalrecord.domain.MedicalRecordSnapshot;
import com.healthcare.medicalrecord.dto.CreateMedicalRecordRequest;
import com.healthcare.medicalrecord.dto.MedicalRecordResponse;
import com.healthcare.medicalrecord.event.MedicalRecordSynchronizedEvent;
import com.healthcare.medicalrecord.exception.ResourceNotFoundException;
import com.healthcare.medicalrecord.integration.FhirAdapter;
import com.healthcare.medicalrecord.repository.MedicalRecordRepository;
import com.healthcare.platform.common.messaging.MessagingPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class MedicalRecordApplicationServiceImpl implements MedicalRecordApplicationService {
    private final MedicalRecordRepository repository;
    private final MessagingPort messagingPort;
    private final FhirAdapter integration;

    public MedicalRecordApplicationServiceImpl(MedicalRecordRepository repository, MessagingPort messagingPort, FhirAdapter integration) {
        this.repository = repository;
        this.messagingPort = messagingPort;
        this.integration = integration;
    }

    @Override
    public MedicalRecordResponse syncMedicalRecord(CreateMedicalRecordRequest request, String correlationId) {
        MedicalRecordSnapshot aggregate = repository.save(new MedicalRecordSnapshot(
                UUID.randomUUID().toString(),
                "SYNCED",
                request.patientId(),
        request.fhirResourceType(),
        request.resourceReference(),
        request.summary()
        ));
        integration.upsertFhirResource(aggregate, correlationId);
        messagingPort.publish("medical-record-service", correlationId, new MedicalRecordSynchronizedEvent(
                aggregate.id(),
                aggregate.patientId(),
                aggregate.fhirResourceType(),
                aggregate.resourceReference(),
                aggregate.summary()
        ));
        return map(aggregate);
    }

    @Override
    public MedicalRecordResponse getMedicalRecord(String id) {
        return repository.findById(id).map(this::map)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalRecord record not found: " + id));
    }

    @Override
    public List<MedicalRecordResponse> listMedicalRecords() {
        return repository.findAll().stream().map(this::map).toList();
    }


    private MedicalRecordResponse map(MedicalRecordSnapshot aggregate) {
        return new MedicalRecordResponse(
                aggregate.id(),
                aggregate.status(),
                aggregate.patientId(),
        aggregate.fhirResourceType(),
        aggregate.resourceReference(),
        aggregate.summary()
        );
    }
}
