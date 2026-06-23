package com.healthcare.careplan.service;

import com.healthcare.careplan.domain.CarePlanAggregate;
import com.healthcare.careplan.dto.CarePlanResponsibilityResponse;
import com.healthcare.careplan.dto.CarePlanResponse;
import com.healthcare.careplan.dto.CreateCarePlanRequest;
import com.healthcare.careplan.dto.UpdateCarePlanRequest;
import com.healthcare.careplan.event.CarePlanCreatedEvent;
import com.healthcare.careplan.event.CarePlanUpdatedEvent;
import com.healthcare.careplan.exception.ProtocolValidationException;
import com.healthcare.careplan.exception.ResourceNotFoundException;
import com.healthcare.careplan.exception.VersionConflictException;
import com.healthcare.careplan.integration.CarePlanFhirAdapter;
import com.healthcare.careplan.integration.CarePlanNotificationAdapter;
import com.healthcare.careplan.repository.CarePlanRepository;
import com.healthcare.platform.common.audit.AuditLogger;
import com.healthcare.platform.common.messaging.MessagingPort;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CarePlanApplicationServiceImpl implements CarePlanApplicationService {
    private static final Set<String> ALLOWED_PLAN_STATUSES = Set.of("DRAFT", "ACTIVE", "ON_HOLD", "COMPLETED", "CANCELLED");

    private final CarePlanRepository repository;
    private final MessagingPort messagingPort;
    private final CarePlanFhirAdapter fhirAdapter;
    private final CarePlanNotificationAdapter notificationAdapter;
    private final AuditLogger auditLogger;

    public CarePlanApplicationServiceImpl(
            CarePlanRepository repository,
            MessagingPort messagingPort,
            CarePlanFhirAdapter fhirAdapter,
            CarePlanNotificationAdapter notificationAdapter,
            AuditLogger auditLogger) {
        this.repository = repository;
        this.messagingPort = messagingPort;
        this.fhirAdapter = fhirAdapter;
        this.notificationAdapter = notificationAdapter;
        this.auditLogger = auditLogger;
    }

    @Override
    public CarePlanResponse createCarePlan(CreateCarePlanRequest request, String correlationId) {
        validateProtocol(request.planStatus(), request.tasks());

        CarePlanAggregate aggregate = repository.save(new CarePlanAggregate(
                UUID.randomUUID().toString(),
                "MANAGED",
                request.patientId(),
                request.goal().trim(),
                request.planStatus().trim().toUpperCase(),
                request.ownerId().trim(),
                normalizeTasks(request.tasks()),
                1
        ));

        try {
            fhirAdapter.synchronizeCarePlan(aggregate, correlationId);
            notificationAdapter.sendCarePlanNotification(aggregate, "CREATED", correlationId);
            messagingPort.publish("careplan-service", correlationId, new CarePlanCreatedEvent(
                    aggregate.id(),
                    aggregate.patientId(),
                    aggregate.goal(),
                    aggregate.planStatus(),
                    aggregate.ownerId(),
                    aggregate.tasks(),
                    aggregate.version()
            ));
                auditLogger.log("SYSTEM", "CAREPLAN_CREATED", aggregate.id(), correlationId);
            return map(aggregate);
        } catch (RuntimeException exception) {
            repository.deleteById(aggregate.id());
            throw exception;
        }
    }

    @Override
    public CarePlanResponse updateCarePlan(String id, UpdateCarePlanRequest request, String correlationId) {
        validateProtocol(request.planStatus(), request.tasks());

        CarePlanAggregate existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CarePlan record not found: " + id));

        if (existing.version() != request.expectedVersion()) {
            throw new VersionConflictException(id, request.expectedVersion(), existing.version());
        }

        CarePlanAggregate updated = repository.save(new CarePlanAggregate(
                existing.id(),
                existing.status(),
                existing.patientId(),
                request.goal().trim(),
                request.planStatus().trim().toUpperCase(),
                request.ownerId().trim(),
                normalizeTasks(request.tasks()),
                existing.version() + 1
        ));

        fhirAdapter.synchronizeCarePlan(updated, correlationId);
        notificationAdapter.sendCarePlanNotification(updated, "UPDATED", correlationId);
        messagingPort.publish("careplan-service", correlationId, new CarePlanUpdatedEvent(
                updated.id(),
                updated.patientId(),
                updated.goal(),
                updated.planStatus(),
                updated.ownerId(),
                updated.tasks(),
                updated.version()
        ));
        auditLogger.log("SYSTEM", "CAREPLAN_UPDATED", updated.id(), correlationId);

        return map(updated);
    }

    @Override
    public CarePlanResponse getCarePlan(String id) {
        return repository.findById(id).map(this::map)
                .orElseThrow(() -> new ResourceNotFoundException("CarePlan record not found: " + id));
    }

    @Override
    public List<CarePlanResponse> listCarePlans() {
        return repository.findAll().stream().map(this::map).toList();
    }

    @Override
    public CarePlanResponsibilityResponse getCarePlanResponsibility(String patientId) {
        CarePlanAggregate latestPlan = repository.findLatestByPatientId(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("CarePlan responsibility not found for patient: " + patientId));

        return new CarePlanResponsibilityResponse(
                latestPlan.patientId(),
                latestPlan.ownerId(),
                latestPlan.id(),
                latestPlan.planStatus(),
                latestPlan.version()
        );
    }

    public CarePlanResponse createManagedCarePlan(CreateCarePlanRequest request, String correlationId) {
        return createCarePlan(request, correlationId);
    }

    private void validateProtocol(String planStatus, List<String> tasks) {
        String normalizedStatus = planStatus == null ? "" : planStatus.trim().toUpperCase();
        if (!ALLOWED_PLAN_STATUSES.contains(normalizedStatus)) {
            throw new ProtocolValidationException("Unsupported planStatus: " + planStatus);
        }
        if (tasks == null || tasks.isEmpty()) {
            throw new ProtocolValidationException("Care plan must include at least one task");
        }
    }

    private List<String> normalizeTasks(List<String> tasks) {
        Set<String> uniqueTasks = new LinkedHashSet<>();
        for (String task : tasks) {
            if (task != null && !task.trim().isBlank()) {
                uniqueTasks.add(task.trim());
            }
        }
        if (uniqueTasks.isEmpty()) {
            throw new ProtocolValidationException("Care plan must include at least one valid task");
        }
        return List.copyOf(uniqueTasks);
    }

    private CarePlanResponse map(CarePlanAggregate aggregate) {
        return new CarePlanResponse(
                aggregate.id(),
                aggregate.status(),
                aggregate.patientId(),
                aggregate.goal(),
                aggregate.planStatus(),
                aggregate.ownerId(),
                aggregate.tasks(),
                aggregate.version()
        );
    }
}
