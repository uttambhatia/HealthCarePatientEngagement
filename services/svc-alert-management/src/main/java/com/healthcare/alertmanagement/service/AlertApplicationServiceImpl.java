package com.healthcare.alertmanagement.service;

import com.healthcare.alertmanagement.domain.ClinicalAlert;
import com.healthcare.alertmanagement.dto.CreateAlertRequest;
import com.healthcare.alertmanagement.dto.AlertResponse;
import com.healthcare.alertmanagement.event.AlertTriggeredEvent;
import com.healthcare.alertmanagement.exception.AlertEscalationFailedException;
import com.healthcare.alertmanagement.exception.InvalidAlertMetricException;
import com.healthcare.alertmanagement.exception.ResourceNotFoundException;
import com.healthcare.alertmanagement.integration.AlertEscalationAdapter;
import com.healthcare.alertmanagement.repository.AlertRepository;
import com.healthcare.platform.common.messaging.MessagingPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AlertApplicationServiceImpl implements AlertApplicationService {
    private final AlertRepository repository;
    private final MessagingPort messagingPort;
    private final AlertEscalationAdapter integration;
    private final double heartRateMax;
    private final double glucoseMax;
    private final double bloodPressureSystolicMax;
    private final double spo2Min;

    public AlertApplicationServiceImpl(
            AlertRepository repository,
            MessagingPort messagingPort,
            AlertEscalationAdapter integration,
            @Value("${platform.alert.rules.hr.max:100}") double heartRateMax,
            @Value("${platform.alert.rules.glucose.max:140}") double glucoseMax,
            @Value("${platform.alert.rules.bp-systolic.max:140}") double bloodPressureSystolicMax,
            @Value("${platform.alert.rules.spo2.min:92}") double spo2Min) {
        this.repository = repository;
        this.messagingPort = messagingPort;
        this.integration = integration;
        this.heartRateMax = heartRateMax;
        this.glucoseMax = glucoseMax;
        this.bloodPressureSystolicMax = bloodPressureSystolicMax;
        this.spo2Min = spo2Min;
    }

    @Override
    public AlertResponse triggerAlert(CreateAlertRequest request, String correlationId) {
        if (!isThresholdBreached(request.triggerType(), request.metricValue())) {
            ClinicalAlert suppressed = repository.save(new ClinicalAlert(
                    UUID.randomUUID().toString(),
                    "SUPPRESSED",
                    request.patientId(),
                    request.deviceId(),
                    request.severity(),
                    request.triggerType(),
                    request.summary()
            ));
            return map(suppressed);
        }

        ClinicalAlert aggregate = repository.save(new ClinicalAlert(
                UUID.randomUUID().toString(),
                "OPEN",
                request.patientId(),
            request.deviceId(),
                request.severity(),
                request.triggerType(),
                request.summary()
        ));

        try {
            integration.escalateAlert(aggregate, correlationId);
        } catch (RuntimeException ex) {
            repository.save(new ClinicalAlert(
                    aggregate.id(),
                    "ESCALATION_FAILED",
                    aggregate.patientId(),
                    aggregate.deviceId(),
                    aggregate.severity(),
                    aggregate.triggerType(),
                    aggregate.summary()
            ));
            throw new AlertEscalationFailedException(aggregate.id(), ex);
        }

        messagingPort.publish("alert-management-service", correlationId, new AlertTriggeredEvent(
                aggregate.id(),
                aggregate.patientId(),
            aggregate.deviceId(),
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
            aggregate.deviceId(),
                aggregate.severity(),
                aggregate.triggerType(),
                aggregate.summary()
        );
    }

    private boolean isThresholdBreached(String triggerType, String metricValue) {
        if (metricValue == null || metricValue.isBlank()) {
            // Backward compatibility for explicit/manual alerts.
            return true;
        }

        double value;
        try {
            value = Double.parseDouble(metricValue);
        } catch (NumberFormatException ex) {
            throw new InvalidAlertMetricException("metricValue must be numeric when provided");
        }

        String normalizedType = triggerType == null ? "" : triggerType.trim().toUpperCase(Locale.ROOT);
        return switch (normalizedType) {
            case "HEART_RATE", "HR" -> value > heartRateMax;
            case "GLUCOSE" -> value > glucoseMax;
            case "BP", "BP_SYS", "BLOOD_PRESSURE", "BLOOD_PRESSURE_SYSTOLIC" -> value > bloodPressureSystolicMax;
            case "SPO2" -> value < spo2Min;
            default -> true;
        };
    }
}
