package com.healthcare.alertmanagement.event;

import com.healthcare.alertmanagement.dto.AlertResponse;
import com.healthcare.alertmanagement.repository.DevicePatientMappingEntity;
import com.healthcare.alertmanagement.repository.JpaDevicePatientMappingRepository;
import com.healthcare.alertmanagement.service.AlertApplicationService;
import com.healthcare.platform.common.event.MessageEnvelope;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "platform.alert.rules.hr.max=100",
    "platform.integration.device-patient.mappings=dev-tel-1001:pat-tel-1001,dev-tel-1002:pat-tel-1002"
})
class TelemetryAlertEventConsumerIntegrationTest {

    @Autowired
    private TelemetryAlertEventConsumer consumer;

    @Autowired
    private AlertApplicationService alertApplicationService;

    @Autowired
    private JpaDevicePatientMappingRepository mappingRepository;

    @Test
    void shouldCreateOpenAlertFromTelemetryWhenThresholdBreached() {
        String deviceId = "dev-tel-1001";
        MessageEnvelope<TelemetryReceivedEvent> envelope = new MessageEnvelope<>(
                "corr-alt-tel-1",
                "TelemetryReceivedEvent",
                OffsetDateTime.now(),
                new TelemetryReceivedEvent("tel-agg-1", deviceId, "HEART_RATE", "128", "2026-06-22T10:00:00Z")
        );

        consumer.handle(envelope);

        List<AlertResponse> alerts = alertApplicationService.listAlerts();
        assertThat(alerts)
                .anySatisfy(alert -> {
                    assertThat(alert.patientId()).isEqualTo("pat-tel-1001");
                    assertThat(alert.deviceId()).isEqualTo(deviceId);
                    assertThat(alert.triggerType()).isEqualTo("HEART_RATE");
                    assertThat(alert.status()).isEqualTo("OPEN");
                });
    }

    @Test
    void shouldCreateSuppressedAlertFromTelemetryWhenThresholdNotBreached() {
        String deviceId = "dev-tel-1002";
        MessageEnvelope<TelemetryReceivedEvent> envelope = new MessageEnvelope<>(
                "corr-alt-tel-2",
                "TelemetryReceivedEvent",
                OffsetDateTime.now(),
                new TelemetryReceivedEvent("tel-agg-2", deviceId, "HEART_RATE", "88", "2026-06-22T10:01:00Z")
        );

        consumer.handle(envelope);

        List<AlertResponse> alerts = alertApplicationService.listAlerts();
        assertThat(alerts)
                .anySatisfy(alert -> {
                    assertThat(alert.patientId()).isEqualTo("pat-tel-1002");
                    assertThat(alert.deviceId()).isEqualTo(deviceId);
                    assertThat(alert.triggerType()).isEqualTo("HEART_RATE");
                    assertThat(alert.status()).isEqualTo("SUPPRESSED");
                });
    }

    @Test
    void shouldSkipTelemetryAlertWhenDevicePatientMappingMissing() {
        MessageEnvelope<TelemetryReceivedEvent> envelope = new MessageEnvelope<>(
                "corr-alt-tel-3",
                "TelemetryReceivedEvent",
                OffsetDateTime.now(),
                new TelemetryReceivedEvent("tel-agg-3", "dev-tel-unknown", "HEART_RATE", "150", "2026-06-22T10:05:00Z")
        );

        consumer.handle(envelope);

        List<AlertResponse> alerts = alertApplicationService.listAlerts();
        assertThat(alerts)
                .noneSatisfy(alert -> assertThat(alert.patientId()).isEqualTo("dev-tel-unknown"));
    }

    @Test
    void shouldCreateAlertUsingDatabaseDevicePatientMapping() {
        String deviceId = "dev-db-1001";
        mappingRepository.save(new DevicePatientMappingEntity(deviceId, "pat-db-1001"));

        MessageEnvelope<TelemetryReceivedEvent> envelope = new MessageEnvelope<>(
                "corr-alt-tel-4",
                "TelemetryReceivedEvent",
                OffsetDateTime.now(),
                new TelemetryReceivedEvent("tel-agg-4", deviceId, "HEART_RATE", "126", "2026-06-22T10:06:00Z")
        );

        consumer.handle(envelope);

        List<AlertResponse> alerts = alertApplicationService.listAlerts();
        assertThat(alerts)
                .anySatisfy(alert -> {
                    assertThat(alert.patientId()).isEqualTo("pat-db-1001");
                    assertThat(alert.deviceId()).isEqualTo(deviceId);
                    assertThat(alert.status()).isEqualTo("OPEN");
                });
    }
}
