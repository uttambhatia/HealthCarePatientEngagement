package com.healthcare.telemetry.dto;

public record TelemetryResponse(
        String id,
        String status,
                String deviceId,
String metricType,
String metricValue,
String recordedAt
) {
}
