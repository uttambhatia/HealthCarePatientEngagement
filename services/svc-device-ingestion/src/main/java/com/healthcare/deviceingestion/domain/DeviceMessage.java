package com.healthcare.deviceingestion.domain;

public record DeviceMessage(
        String id,
        String status,
                String deviceId,
String protocol,
String payload,
String receivedAt
) {
}
