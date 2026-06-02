package com.healthcare.deviceingestion.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "device_ingestion_messages")
public class DeviceMessageEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String deviceId;

    @Column(nullable = false)
    private String protocol;

    @Column(nullable = false)
    private String payload;

    @Column(nullable = false)
    private String receivedAt;

    protected DeviceMessageEntity() {
    }

    public DeviceMessageEntity(String id, String status, String deviceId, String protocol, String payload, String receivedAt) {
        this.id = id;
        this.status = status;
        this.deviceId = deviceId;
        this.protocol = protocol;
        this.payload = payload;
        this.receivedAt = receivedAt;
    }

    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getPayload() {
        return payload;
    }

    public String getReceivedAt() {
        return receivedAt;
    }
}
