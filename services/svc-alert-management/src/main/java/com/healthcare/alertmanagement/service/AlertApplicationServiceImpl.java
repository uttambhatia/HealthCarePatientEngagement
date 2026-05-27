package com.healthcare.alertmanagement.service;

import com.healthcare.alertmanagement.domain.ClinicalAlert;
import com.healthcare.alertmanagement.dto.CreateAlertRequest;
import com.healthcare.alertmanagement.dto.AlertResponse;
import com.healthcare.alertmanagement.event.AlertTriggeredEvent;
import com.healthcare.alertmanagement.exception.ResourceNotFoundException;
import com.healthcare.alertmanagement.integration.AlertEscalationAdapter;
import com.healthcare.alertmanagement.repository.AlertRepository;
import com.healthcare.platform.common.messaging.MessagingPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AlertApplicationServiceImpl implements AlertApplicationService {
    private final AlertRepository repository;
    private final MessagingPort messagingPort;
    private final AlertEscalationAdapter integration;

    public AlertApplicationServiceImpl(AlertRepository repository, MessagingPort messagingPort, AlertEscalationAdapter integration) {
        this.repository = repository;
        this.messagingPort = messagingPort;
        this.integration = integration;
    }

    @Override
    public AlertResponse triggerAlert(CreateAlertRequest request, String correlationId) {
        ClinicalAlert aggregate = repository.save(new ClinicalAlert(
                UUID.randomUUID().toString(),
                "OPEN",
                request.patientId(),
        request.severity(),
        request.triggerType(),
        request.summary()
        ));
        integration.escalateAlert(aggregate, correlationId);
        messagingPort.publish("alert-management-service", correlationId, new AlertTriggeredEvent(
                aggregate.id(),
                aggregate.patientId(),
                aggregate.severity(),
                aggregate.triggerType(),
                aggregate.summary()
        ));
        return map(aggregate);
    }

    @Override
    public AlertResponse getAlert(String id) {
        return repository.findById(id).map(this::map)
                .orElseThrow(() -> new ResourceNotFoundException("AlertManagement record not found: " + id));
    }

    @Override
    public List<AlertResponse> listAlerts() {
        return repository.findAll().stream().map(this::map).toList();
    }


    private AlertResponse map(ClinicalAlert aggregate) {
        return new AlertResponse(
                aggregate.id(),
                aggregate.status(),
                aggregate.patientId(),
        aggregate.severity(),
        aggregate.triggerType(),
        aggregate.summary()
        );
    }
}
