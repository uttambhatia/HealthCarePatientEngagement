package com.healthcare.deviceingestion.service;

import com.healthcare.deviceingestion.dto.CreateDeviceIngestionRequest;
import com.healthcare.deviceingestion.dto.DeviceIngestionResponse;

import java.util.List;

public interface DeviceIngestionApplicationService {
    DeviceIngestionResponse ingestDeviceSignal(CreateDeviceIngestionRequest request, String correlationId);
    DeviceIngestionResponse getDeviceSignal(String id);
    List<DeviceIngestionResponse> listDeviceSignals();
}
