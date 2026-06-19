package com.healthcare.patient.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "patient_profiles")
public class PatientProfileEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String status;

    @Column(length = 512)
    private String decisionAudit;

    @Column(nullable = false, unique = true)
    private String externalReference;

    @Column(nullable = false)
    private String givenName;

    @Column(nullable = false)
    private String familyName;

    @Column(nullable = false)
    private String birthDate;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String demographics;

    @Column(length = 512)
    private String idProofBlobName;

    @Column(length = 256)
    private String idProofFileName;

    protected PatientProfileEntity() {
    }

    public PatientProfileEntity(String id, String status, String decisionAudit, String externalReference, String givenName, String familyName,
                                String birthDate, String email, String phone, String demographics,
                                String idProofBlobName, String idProofFileName) {
        this.id = id;
        this.status = status;
        this.decisionAudit = decisionAudit;
        this.externalReference = externalReference;
        this.givenName = givenName;
        this.familyName = familyName;
        this.birthDate = birthDate;
        this.email = email;
        this.phone = phone;
        this.demographics = demographics;
        this.idProofBlobName = idProofBlobName;
        this.idProofFileName = idProofFileName;
    }

    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public String getDecisionAudit() {
        return decisionAudit;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getDemographics() {
        return demographics;
    }

    public String getIdProofBlobName() {
        return idProofBlobName;
    }

    public String getIdProofFileName() {
        return idProofFileName;
    }
}