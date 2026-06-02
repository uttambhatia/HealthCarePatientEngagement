package com.healthcare.telemetry.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "device_patient_mapping")
public class DevicePatientMappingEntity {
    @Id
    @Column(name = "device_id", nullable = false, updatable = false)
    private String deviceId;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    protected DevicePatientMappingEntity() {
    }

    public DevicePatientMappingEntity(String deviceId, String patientId) {
        this.deviceId = deviceId;
        this.patientId = patientId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getPatientId() {
        return patientId;
    }
}
