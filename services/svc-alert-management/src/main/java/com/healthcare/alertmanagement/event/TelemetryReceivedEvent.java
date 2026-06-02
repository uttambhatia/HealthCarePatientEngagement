package com.healthcare.alertmanagement.event;

public record TelemetryReceivedEvent(
        String aggregateId,
        String deviceId,
        String metricType,
        String metricValue,
        String recordedAt
) {
}
