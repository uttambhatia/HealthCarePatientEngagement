package com.healthcare.telemetry.service;

import com.healthcare.platform.common.messaging.MessagingPort;
import com.healthcare.telemetry.domain.TelemetryReading;
import com.healthcare.telemetry.integration.TelemetryTimeSeriesAdapter;
import com.healthcare.telemetry.repository.DevicePatientMappingEntity;
import com.healthcare.telemetry.repository.JpaDevicePatientMappingRepository;
import com.healthcare.telemetry.repository.TelemetryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryApplicationServiceImplTest {

    @Mock
    private TelemetryRepository telemetryRepository;

    @Mock
    private JpaDevicePatientMappingRepository devicePatientMappingRepository;

    @Mock
    private MessagingPort messagingPort;

    private TelemetryApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        TelemetryTimeSeriesAdapter telemetryTimeSeriesAdapter =
                new TelemetryTimeSeriesAdapter(RestClient.builder(), "", "/timeseries/telemetry", 3);
        service = new TelemetryApplicationServiceImpl(
                telemetryRepository,
                devicePatientMappingRepository,
                messagingPort,
                telemetryTimeSeriesAdapter
        );
    }

    @Test
    void listTelemetryByPatientReturnsEmptyWhenNoDeviceMappings() {
        when(devicePatientMappingRepository.findByPatientId("pat-1")).thenReturn(List.of());

        var result = service.listTelemetryByPatient("pat-1", null, null, null);

        assertEquals(0, result.size());
    }

    @Test
    void listTelemetryByPatientFiltersByMetricTypeCaseInsensitive() {
        when(devicePatientMappingRepository.findByPatientId("pat-1")).thenReturn(List.of(
                new DevicePatientMappingEntity("dev-1", "pat-1"),
                new DevicePatientMappingEntity("dev-2", "pat-1")
        ));
        when(telemetryRepository.findByDeviceIds(anyList())).thenReturn(List.of(
                new TelemetryReading("t-1", "RECORDED", "dev-1", "HEART_RATE", "78", "2026-06-01T08:00:00Z"),
                new TelemetryReading("t-2", "RECORDED", "dev-2", "spo2", "97", "2026-06-01T08:05:00Z")
        ));

        var result = service.listTelemetryByPatient("pat-1", "heart_rate", null, null);

        assertEquals(1, result.size());
        assertEquals("t-1", result.get(0).id());
    }

    @Test
    void listTelemetryByPatientFiltersByStartAndEndTime() {
        when(devicePatientMappingRepository.findByPatientId("pat-1")).thenReturn(List.of(
                new DevicePatientMappingEntity("dev-1", "pat-1")
        ));
        when(telemetryRepository.findByDeviceIds(anyList())).thenReturn(List.of(
                new TelemetryReading("t-1", "RECORDED", "dev-1", "HEART_RATE", "75", "2026-06-01T07:00:00Z"),
                new TelemetryReading("t-2", "RECORDED", "dev-1", "HEART_RATE", "82", "2026-06-01T08:30:00Z"),
                new TelemetryReading("t-3", "RECORDED", "dev-1", "HEART_RATE", "79", "2026-06-01T10:00:00Z")
        ));

        var result = service.listTelemetryByPatient(
                "pat-1",
                null,
                "2026-06-01T08:00:00Z",
                "2026-06-01T09:00:00Z"
        );

        assertEquals(1, result.size());
        assertEquals("t-2", result.get(0).id());
    }
}
