package com.healthcare.appointment.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "appointment_records",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_appointment_provider_schedule", columnNames = {"providerId", "scheduledAt"})
    }
)
public class AppointmentRecordEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String patientId;

    @Column(nullable = false)
    private String providerId;

    @Column(nullable = false)
    private String scheduledAt;

    @Column(nullable = false)
    private String channel;

    protected AppointmentRecordEntity() {
    }

    public AppointmentRecordEntity(String id, String status, String patientId, String providerId, String scheduledAt, String channel) {
        this.id = id;
        this.status = status;
        this.patientId = patientId;
        this.providerId = providerId;
        this.scheduledAt = scheduledAt;
        this.channel = channel;
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

    public String getProviderId() {
        return providerId;
    }

    public String getScheduledAt() {
        return scheduledAt;
    }

    public String getChannel() {
        return channel;
    }
}