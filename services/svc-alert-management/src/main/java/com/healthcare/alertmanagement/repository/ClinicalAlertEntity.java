package com.healthcare.alertmanagement.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "clinical_alerts")
public class ClinicalAlertEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "severity", nullable = false)
    private String severity;

    @Column(name = "trigger_type", nullable = false)
    private String triggerType;

    @Column(name = "summary", nullable = false)
    private String summary;

    protected ClinicalAlertEntity() {
    }

    public ClinicalAlertEntity(String id, String status, String patientId, String deviceId, String severity, String triggerType, String summary) {
        this.id = id;
        this.status = status;
        this.patientId = patientId;
        this.deviceId = deviceId;
        this.severity = severity;
        this.triggerType = triggerType;
        this.summary = summary;
    }

    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getSeverity() {
        return severity;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public String getSummary() {
        return summary;
    }
}
