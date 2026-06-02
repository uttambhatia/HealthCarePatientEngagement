package com.healthcare.telemetry.repository;

import com.healthcare.telemetry.domain.TelemetryReading;

import java.util.List;
import java.util.Optional;

public interface TelemetryRepository {
    TelemetryReading save(TelemetryReading aggregate);
    Optional<TelemetryReading> findById(String id);
    List<TelemetryReading> findAll();
    List<TelemetryReading> findByDeviceIds(List<String> deviceIds);
    void deleteById(String id);
}
