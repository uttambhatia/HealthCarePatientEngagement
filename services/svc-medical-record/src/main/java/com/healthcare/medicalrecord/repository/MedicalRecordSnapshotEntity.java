package com.healthcare.medicalrecord.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "medical_record_snapshots")
public class MedicalRecordSnapshotEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String patientId;

    @Column(nullable = false)
    private String fhirResourceType;

    @Column(nullable = false)
    private String resourceReference;

    @Column(nullable = false)
    private String summary;

    @Column(nullable = false)
    private int version;

    protected MedicalRecordSnapshotEntity() {
    }

    public MedicalRecordSnapshotEntity(String id, String status, String patientId, String fhirResourceType, String resourceReference, String summary, int version) {
        this.id = id;
        this.status = status;
        this.patientId = patientId;
        this.fhirResourceType = fhirResourceType;
        this.resourceReference = resourceReference;
        this.summary = summary;
        this.version = version;
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

    public String getFhirResourceType() {
        return fhirResourceType;
    }

    public String getResourceReference() {
        return resourceReference;
    }

    public String getSummary() {
        return summary;
    }

    public int getVersion() {
        return version;
    }
}