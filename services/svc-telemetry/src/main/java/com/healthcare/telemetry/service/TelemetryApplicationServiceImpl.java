package com.healthcare.telemetry.service;

import com.healthcare.telemetry.domain.TelemetryReading;
import com.healthcare.telemetry.dto.CreateTelemetryRequest;
import com.healthcare.telemetry.dto.TelemetryResponse;
import com.healthcare.telemetry.event.TelemetryReceivedEvent;
import com.healthcare.telemetry.exception.ResourceNotFoundException;
import com.healthcare.telemetry.integration.TelemetryTimeSeriesAdapter;
import com.healthcare.telemetry.repository.TelemetryRepository;
import com.healthcare.platform.common.messaging.MessagingPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TelemetryApplicationServiceImpl implements TelemetryApplicationService {
    private final TelemetryRepository repository;
    private final MessagingPort messagingPort;
    private final TelemetryTimeSeriesAdapter integration;

    public TelemetryApplicationServiceImpl(TelemetryRepository repository, MessagingPort messagingPort, TelemetryTimeSeriesAdapter integration) {
        this.repository = repository;
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
