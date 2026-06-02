package com.healthcare.deviceingestion.service;

import com.healthcare.deviceingestion.domain.DeviceMessage;
import com.healthcare.deviceingestion.dto.CreateDeviceIngestionRequest;
import com.healthcare.deviceingestion.dto.DeviceIngestionResponse;
import com.healthcare.deviceingestion.event.DeviceSignalIngestedEvent;
import com.healthcare.deviceingestion.exception.InvalidTelemetryPayloadException;
import com.healthcare.deviceingestion.exception.ResourceNotFoundException;
import com.healthcare.deviceingestion.exception.TelemetryForwardingFailedException;
import com.healthcare.deviceingestion.exception.UnregisteredDeviceException;
import com.healthcare.deviceingestion.integration.IoTAdapter;
import com.healthcare.deviceingestion.integration.TelemetryIngestionAdapter;
import com.healthcare.deviceingestion.repository.DeviceIngestionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.platform.common.messaging.MessagingPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class DeviceIngestionApplicationServiceImpl implements DeviceIngestionApplicationService {
    private static final Set<String> SUPPORTED_PROTOCOLS = new LinkedHashSet<>(List.of("MQTT", "HTTP", "AMQP"));
    private static final List<String> SUPPORTED_METRIC_KEYS = List.of("bp", "hr", "glucose", "spo2", "temperature");

    private final DeviceIngestionRepository repository;
    private final MessagingPort messagingPort;
    private final IoTAdapter integration;
    private final TelemetryIngestionAdapter telemetryIngestionAdapter;
    private final ObjectMapper objectMapper;
    private final Set<String> registeredDeviceIds;

    public DeviceIngestionApplicationServiceImpl(
            DeviceIngestionRepository repository,
            MessagingPort messagingPort,
            IoTAdapter integration,
            TelemetryIngestionAdapter telemetryIngestionAdapter,
            ObjectMapper objectMapper,
            @Value("${platform.deviceingestion.registered-device-ids:}") List<String> registeredDeviceIds) {
        this.repository = repository;
        this.messagingPort = messagingPort;
        this.integration = integration;
        this.telemetryIngestionAdapter = telemetryIngestionAdapter;
        this.objectMapper = objectMapper;
        this.registeredDeviceIds = new LinkedHashSet<>(registeredDeviceIds);
    }

    @Override
    public DeviceIngestionResponse ingestDeviceSignal(CreateDeviceIngestionRequest request, String correlationId) {
        validateRegisteredDevice(request.deviceId());

        String normalizedProtocol = normalizeProtocol(request.protocol());
        validateProtocol(normalizedProtocol);
        validateReceivedAt(request.receivedAt());

        Map<String, Object> payloadMap = parsePayload(request.payload());
        Map.Entry<String, String> telemetry = extractTelemetryMetric(payloadMap);

        DeviceMessage received = repository.save(new DeviceMessage(
                UUID.randomUUID().toString(),
            "RECEIVED",
                request.deviceId(),
            normalizedProtocol,
            request.payload(),
            request.receivedAt()
        ));

        try {
            integration.ingest(received, correlationId);
            telemetryIngestionAdapter.forwardToTelemetry(received, telemetry.getKey(), telemetry.getValue(), correlationId);
        } catch (RuntimeException ex) {
            repository.save(new DeviceMessage(
                received.id(),
                "FAILED",
                received.deviceId(),
                received.protocol(),
                received.payload(),
                received.receivedAt()
            ));
            throw new TelemetryForwardingFailedException(received.id(), ex);
        }

        DeviceMessage aggregate = repository.save(new DeviceMessage(
            received.id(),
            "INGESTED",
            received.deviceId(),
            received.protocol(),
            received.payload(),
            received.receivedAt()
        ));

        messagingPort.publish("device-ingestion-service", correlationId, new DeviceSignalIngestedEvent(
                aggregate.id(),
                aggregate.deviceId(),
                aggregate.protocol(),
                aggregate.payload(),
                aggregate.receivedAt()
        ));
        return map(aggregate);
    }

    @Override
    public DeviceIngestionResponse getDeviceSignal(String id) {
        return repository.findById(id).map(this::map)
                .orElseThrow(() -> new ResourceNotFoundException("DeviceIngestion record not found: " + id));
    }

    @Override
    public List<DeviceIngestionResponse> listDeviceSignals() {
        return repository.findAll().stream().map(this::map).toList();
    }


    private DeviceIngestionResponse map(DeviceMessage aggregate) {
        return new DeviceIngestionResponse(
                aggregate.id(),
                aggregate.status(),
                aggregate.deviceId(),
                aggregate.protocol(),
                aggregate.payload(),
                aggregate.receivedAt()
        );
    }

    private void validateRegisteredDevice(String deviceId) {
        if (!registeredDeviceIds.isEmpty() && !registeredDeviceIds.contains(deviceId)) {
            throw new UnregisteredDeviceException(deviceId);
        }
    }

    private String normalizeProtocol(String protocol) {
        return protocol == null ? "" : protocol.trim().toUpperCase(Locale.ROOT);
    }

    private void validateProtocol(String protocol) {
        if (!SUPPORTED_PROTOCOLS.contains(protocol)) {
            throw new InvalidTelemetryPayloadException("Unsupported protocol: " + protocol);
        }
    }

    private void validateReceivedAt(String receivedAt) {
        try {
            OffsetDateTime.parse(receivedAt);
        } catch (RuntimeException ex) {
            throw new InvalidTelemetryPayloadException("receivedAt must be ISO-8601 date-time");
        }
    }

    private Map<String, Object> parsePayload(String payload) {
        try {
            Map<String, Object> parsed = objectMapper.readValue(payload, new TypeReference<>() {});
            if (parsed.isEmpty()) {
                throw new InvalidTelemetryPayloadException("payload must contain at least one metric");
            }
            return parsed;
        } catch (IOException ex) {
            throw new InvalidTelemetryPayloadException("payload must be a valid JSON object");
        }
    }

    private Map.Entry<String, String> extractTelemetryMetric(Map<String, Object> payload) {
        for (String key : SUPPORTED_METRIC_KEYS) {
            Object value = payload.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return Map.entry(key.toUpperCase(Locale.ROOT), String.valueOf(value));
            }
        }
        throw new InvalidTelemetryPayloadException("payload must include at least one supported metric: " + String.join(", ", SUPPORTED_METRIC_KEYS));
    }
}
