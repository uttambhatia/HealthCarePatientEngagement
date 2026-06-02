package com.healthcare.telemetry.service;

import com.healthcare.telemetry.domain.TelemetryReading;
import com.healthcare.telemetry.dto.CreateTelemetryRequest;
import com.healthcare.telemetry.dto.TelemetryResponse;
import com.healthcare.telemetry.event.TelemetryReceivedEvent;
import com.healthcare.telemetry.exception.ResourceNotFoundException;
import com.healthcare.telemetry.integration.TelemetryTimeSeriesAdapter;
import com.healthcare.telemetry.repository.JpaDevicePatientMappingRepository;
import com.healthcare.telemetry.repository.TelemetryRepository;
import com.healthcare.platform.common.messaging.MessagingPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TelemetryApplicationServiceImpl implements TelemetryApplicationService {
    private final TelemetryRepository repository;
    private final JpaDevicePatientMappingRepository devicePatientMappingRepository;
    private final MessagingPort messagingPort;
    private final TelemetryTimeSeriesAdapter integration;

    public TelemetryApplicationServiceImpl(
            TelemetryRepository repository,
            JpaDevicePatientMappingRepository devicePatientMappingRepository,
            MessagingPort messagingPort,
            TelemetryTimeSeriesAdapter integration) {
        this.repository = repository;
        this.devicePatientMappingRepository = devicePatientMappingRepository;
        this.messagingPort = messagingPort;
        this.integration = integration;
    }

    @Override
    public TelemetryResponse recordTelemetry(CreateTelemetryRequest request, String correlationId) {
        TelemetryReading aggregate = repository.save(new TelemetryReading(
                UUID.randomUUID().toString(),
                "RECORDED",
                request.deviceId(),
        request.metricType(),
        request.metricValue(),
        request.recordedAt()
        ));
        integration.persistMetric(aggregate, correlationId);
        messagingPort.publish("telemetry-service", correlationId, new TelemetryReceivedEvent(
                aggregate.id(),
                aggregate.deviceId(),
                aggregate.metricType(),
                aggregate.metricValue(),
                aggregate.recordedAt()
        ));
        return map(aggregate);
    }

    @Override
    public TelemetryResponse getTelemetry(String id) {
        return repository.findById(id).map(this::map)
                .orElseThrow(() -> new ResourceNotFoundException("Telemetry record not found: " + id));
    }

    @Override
    public List<TelemetryResponse> listTelemetry() {
        return repository.findAll().stream().map(this::map).toList();
    }

    @Override
    public List<TelemetryResponse> listTelemetryByPatient(String patientId, String metricType, String startTime, String endTime) {
        List<String> deviceIds = devicePatientMappingRepository.findByPatientId(patientId).stream()
                .map(mapping -> mapping.getDeviceId())
                .toList();

        if (deviceIds.isEmpty()) {
            return List.of();
        }

        String normalizedMetricType = metricType == null ? null : metricType.trim();
        return repository.findByDeviceIds(deviceIds).stream()
                .filter(reading -> normalizedMetricType == null || normalizedMetricType.isEmpty()
                        || reading.metricType().equalsIgnoreCase(normalizedMetricType))
                .filter(reading -> startTime == null || startTime.isBlank() || reading.recordedAt().compareTo(startTime) >= 0)
                .filter(reading -> endTime == null || endTime.isBlank() || reading.recordedAt().compareTo(endTime) <= 0)
                .map(this::map)
                .toList();
    }

    @Override
    public List<String> listMetricTypes(String patientId) {
        boolean filterByPatient = patientId != null && !patientId.isBlank();

        List<TelemetryReading> rows;
        if (filterByPatient) {
            List<String> deviceIds = devicePatientMappingRepository.findByPatientId(patientId.trim()).stream()
                    .map(mapping -> mapping.getDeviceId())
                    .toList();
            if (deviceIds.isEmpty()) {
                return List.of();
            }
            rows = repository.findByDeviceIds(deviceIds);
        } else {
            rows = repository.findAll();
        }

        return rows.stream()
                .map(TelemetryReading::metricType)
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isEmpty())
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .toList();
    }


    private TelemetryResponse map(TelemetryReading aggregate) {
        return new TelemetryResponse(
                aggregate.id(),
                aggregate.status(),
                aggregate.deviceId(),
        aggregate.metricType(),
        aggregate.metricValue(),
        aggregate.recordedAt()
        );
    }
}
