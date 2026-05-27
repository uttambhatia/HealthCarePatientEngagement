package com.healthcare.telemetry.service;

import com.healthcare.telemetry.dto.CreateTelemetryRequest;
import com.healthcare.telemetry.dto.TelemetryResponse;

import java.util.List;

public interface TelemetryApplicationService {
    TelemetryResponse recordTelemetry(CreateTelemetryRequest request, String correlationId);
    TelemetryResponse getTelemetry(String id);
    List<TelemetryResponse> listTelemetry();
}
