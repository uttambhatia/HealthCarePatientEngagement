package com.healthcare.medicalrecord.service;

import com.healthcare.medicalrecord.domain.MedicalRecordSnapshot;
import com.healthcare.medicalrecord.dto.CreateMedicalRecordRequest;
import com.healthcare.medicalrecord.dto.MedicalRecordResponse;
import com.healthcare.medicalrecord.dto.UpdateMedicalRecordRequest;
import com.healthcare.medicalrecord.event.MedicalRecordSynchronizedEvent;
import com.healthcare.medicalrecord.exception.FhirValidationException;
import com.healthcare.medicalrecord.exception.ResourceNotFoundException;
import com.healthcare.medicalrecord.exception.VersionConflictException;
import com.healthcare.medicalrecord.integration.FhirAdapter;
import com.healthcare.medicalrecord.repository.MedicalRecordRepository;
import com.healthcare.platform.common.messaging.MessagingPort;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class MedicalRecordApplicationServiceImpl implements MedicalRecordApplicationService {
    private static final Set<String> ALLOWED_FHIR_RESOURCE_TYPES = new LinkedHashSet<>(List.of(
            "Observation",
            "Condition",
            "DiagnosticReport",
            "Encounter",
            "MedicationRequest",
            "Procedure",
            "CarePlan",
            "AllergyIntolerance"
    ));

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
        validateFhirResourceType(request.fhirResourceType());

        MedicalRecordSnapshot aggregate = repository.save(new MedicalRecordSnapshot(
                UUID.randomUUID().toString(),
                "SYNCED",
                request.patientId(),
            request.fhirResourceType(),
            request.resourceReference(),
            request.summary(),
            1
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
    public MedicalRecordResponse updateMedicalRecord(String id, UpdateMedicalRecordRequest request, String correlationId) {
        validateFhirResourceType(request.fhirResourceType());

        MedicalRecordSnapshot existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalRecord record not found: " + id));

        if (existing.version() != request.expectedVersion()) {
            throw new VersionConflictException(id, request.expectedVersion(), existing.version());
        }

        MedicalRecordSnapshot updated = repository.save(new MedicalRecordSnapshot(
                existing.id(),
                existing.status(),
                existing.patientId(),
                request.fhirResourceType(),
                request.resourceReference(),
                request.summary(),
                existing.version() + 1
        ));

        integration.upsertFhirResource(updated, correlationId);
        messagingPort.publish("medical-record-service", correlationId, new MedicalRecordSynchronizedEvent(
                updated.id(),
                updated.patientId(),
                updated.fhirResourceType(),
                updated.resourceReference(),
                updated.summary()
        ));

        return map(updated);
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
                aggregate.summary(),
                aggregate.version()
        );
    }

    private void validateFhirResourceType(String fhirResourceType) {
        if (!ALLOWED_FHIR_RESOURCE_TYPES.contains(fhirResourceType)) {
            throw new FhirValidationException(fhirResourceType);
        }
    }
}
