package com.healthcare.telemetry.domain;

public record TelemetryReading(
        String id,
        String status,
                String deviceId,
String metricType,
String metricValue,
String recordedAt
) {
}
