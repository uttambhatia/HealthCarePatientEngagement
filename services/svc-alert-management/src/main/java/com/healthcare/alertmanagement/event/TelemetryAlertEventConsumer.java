package com.healthcare.alertmanagement.event;

import com.healthcare.alertmanagement.dto.CreateAlertRequest;
import com.healthcare.alertmanagement.service.AlertApplicationService;
import com.healthcare.alertmanagement.service.DevicePatientResolver;
import com.healthcare.platform.common.event.MessageEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TelemetryAlertEventConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryAlertEventConsumer.class);

    private final AlertApplicationService alertApplicationService;
    private final DevicePatientResolver devicePatientResolver;

    public TelemetryAlertEventConsumer(
            AlertApplicationService alertApplicationService,
            DevicePatientResolver devicePatientResolver) {
        this.alertApplicationService = alertApplicationService;
        this.devicePatientResolver = devicePatientResolver;
    }

    public void handle(MessageEnvelope<TelemetryReceivedEvent> envelope) {
        TelemetryReceivedEvent payload = envelope.payload();
        if (payload == null) {
            LOGGER.warn("Skipping telemetry alert handling because payload is null correlationId={}", envelope.correlationId());
            return;
        }

        String patientId = devicePatientResolver.resolvePatientId(payload.deviceId()).orElse(null);
        if (patientId == null) {
            LOGGER.warn("Skipping telemetry alert handling because patient mapping not found deviceId={} correlationId={}",
                payload.deviceId(), envelope.correlationId());
            return;
        }

        String triggerType = payload.metricType();
        String metricValue = payload.metricValue();
        String summary = "Telemetry " + triggerType + " value=" + metricValue + " detected for device=" + payload.deviceId();

        CreateAlertRequest request = new CreateAlertRequest(
                patientId,
            payload.deviceId(),
                "HIGH",
                triggerType,
                metricValue,
                summary
        );

        alertApplicationService.triggerAlert(request, envelope.correlationId());
        LOGGER.info("Telemetry alert evaluation completed correlationId={} deviceId={} metricType={} metricValue={}",
                envelope.correlationId(), payload.deviceId(), payload.metricType(), payload.metricValue());
    }
}
