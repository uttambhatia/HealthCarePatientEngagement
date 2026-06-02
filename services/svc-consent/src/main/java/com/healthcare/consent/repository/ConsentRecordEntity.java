package com.healthcare.consent.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "consent_records", uniqueConstraints = {
    @UniqueConstraint(name = "uk_consent_patient_type_version", columnNames = {"patientId", "consentType", "version"})
})
public class ConsentRecordEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String patientId;

    @Column(nullable = false)
    private String consentType;

    @Column(nullable = false)
    private boolean granted;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private String effectiveFrom;

    protected ConsentRecordEntity() {
    }

    public ConsentRecordEntity(String id, String status, String patientId, String consentType,
                               boolean granted, int version, String effectiveFrom) {
        this.id = id;
        this.status = status;
        this.patientId = patientId;
        this.consentType = consentType;
        this.granted = granted;
        this.version = version;
        this.effectiveFrom = effectiveFrom;
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

    public String getConsentType() {
        return consentType;
    }

    public boolean isGranted() {
        return granted;
    }

    public int getVersion() {
        return version;
    }

    public String getEffectiveFrom() {
        return effectiveFrom;
    }
}