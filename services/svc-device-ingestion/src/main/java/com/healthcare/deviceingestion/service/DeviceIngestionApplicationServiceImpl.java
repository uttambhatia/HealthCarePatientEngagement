package com.healthcare.deviceingestion.service;

import com.healthcare.deviceingestion.domain.DeviceMessage;
import com.healthcare.deviceingestion.dto.CreateDeviceIngestionRequest;
import com.healthcare.deviceingestion.dto.DeviceIngestionResponse;
import com.healthcare.deviceingestion.event.DeviceSignalIngestedEvent;
import com.healthcare.deviceingestion.exception.ResourceNotFoundException;
import com.healthcare.deviceingestion.integration.IoTAdapter;
import com.healthcare.deviceingestion.repository.DeviceIngestionRepository;
import com.healthcare.platform.common.messaging.MessagingPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DeviceIngestionApplicationServiceImpl implements DeviceIngestionApplicationService {
    private final DeviceIngestionRepository repository;
    private final MessagingPort messagingPort;
    private final IoTAdapter integration;

    public DeviceIngestionApplicationServiceImpl(DeviceIngestionRepository repository, MessagingPort messagingPort, IoTAdapter integration) {
        this.repository = repository;
        this.messagingPort = messagingPort;
        this.integration = integration;
    }

    @Override
    public DeviceIngestionResponse ingestDeviceSignal(CreateDeviceIngestionRequest request, String correlationId) {
        DeviceMessage aggregate = repository.save(new DeviceMessage(
                UUID.randomUUID().toString(),
                "INGESTED",
                request.deviceId(),
        request.protocol(),
        request.payload(),
        request.receivedAt()
        ));
        integration.ingest(aggregate, correlationId);
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
}
