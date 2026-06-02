package com.healthcare.telemetry.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "telemetry_readings")
public class TelemetryReadingEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String deviceId;

    @Column(nullable = false)
    private String metricType;

    @Column(nullable = false)
    private String metricValue;

    @Column(nullable = false)
    private String recordedAt;

    protected TelemetryReadingEntity() {
    }

    public TelemetryReadingEntity(String id, String status, String deviceId, String metricType, String metricValue, String recordedAt) {
        this.id = id;
        this.status = status;
        this.deviceId = deviceId;
        this.metricType = metricType;
        this.metricValue = metricValue;
        this.recordedAt = recordedAt;
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

    public String getMetricType() {
        return metricType;
    }

    public String getMetricValue() {
        return metricValue;
    }

    public String getRecordedAt() {
        return recordedAt;
    }
}