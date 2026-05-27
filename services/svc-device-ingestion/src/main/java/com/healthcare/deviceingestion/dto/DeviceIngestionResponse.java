package com.healthcare.deviceingestion.dto;

public record DeviceIngestionResponse(
        String id,
        String status,
                String deviceId,
String protocol,
String payload,
String receivedAt
) {
}
