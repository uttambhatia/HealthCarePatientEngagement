package com.healthcare.appointment.repository;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "teleconsultation_sessions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_teleconsult_appointment", columnNames = {"appointmentId"})
        }
)
public class TeleconsultationSessionEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String appointmentId;

    @Column(nullable = false)
    private String patientId;

    @Column(nullable = false)
    private String providerId;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String doctorJoinUrl;

    @Column(nullable = false)
    private String patientJoinUrl;

    @Column(nullable = false)
    private String startedAt;

    @Column
    private String joinedAt;

    @Column
    private String completedAt;

    @Column(length = 4000)
    private String consultationNotes;

    @Column(nullable = false)
    private boolean followUpRequired;

    @Column
    private String nextFollowUpDate;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "teleconsultation_prescriptions", joinColumns = @JoinColumn(name = "session_id"))
    @OrderColumn(name = "prescription_order")
    @Column(name = "prescription_entry", nullable = false, length = 500)
    private List<String> prescriptions = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "teleconsultation_interaction_logs", joinColumns = @JoinColumn(name = "session_id"))
    @OrderColumn(name = "log_order")
    @Column(name = "log_entry", nullable = false, length = 500)
    private List<String> interactionLogs = new ArrayList<>();

    protected TeleconsultationSessionEntity() {
    }

    public TeleconsultationSessionEntity(
            String id,
            String appointmentId,
            String patientId,
            String providerId,
            String status,
            String doctorJoinUrl,
            String patientJoinUrl,
            String startedAt,
            String joinedAt,
            String completedAt,
            String consultationNotes,
            boolean followUpRequired,
            String nextFollowUpDate,
            List<String> prescriptions,
            List<String> interactionLogs) {
        this.id = id;
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.providerId = providerId;
        this.status = status;
        this.doctorJoinUrl = doctorJoinUrl;
        this.patientJoinUrl = patientJoinUrl;
        this.startedAt = startedAt;
        this.joinedAt = joinedAt;
        this.completedAt = completedAt;
        this.consultationNotes = consultationNotes;
        this.followUpRequired = followUpRequired;
        this.nextFollowUpDate = nextFollowUpDate;
        this.prescriptions = prescriptions == null ? new ArrayList<>() : new ArrayList<>(prescriptions);
        this.interactionLogs = interactionLogs == null ? new ArrayList<>() : new ArrayList<>(interactionLogs);
    }

    public String getId() {
        return id;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getStatus() {
        return status;
    }

    public String getDoctorJoinUrl() {
        return doctorJoinUrl;
    }

    public String getPatientJoinUrl() {
        return patientJoinUrl;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public String getJoinedAt() {
        return joinedAt;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    public String getConsultationNotes() {
        return consultationNotes;
    }

    public boolean isFollowUpRequired() {
        return followUpRequired;
    }

    public String getNextFollowUpDate() {
        return nextFollowUpDate;
    }

    public List<String> getPrescriptions() {
        return prescriptions;
    }

    public List<String> getInteractionLogs() {
        return interactionLogs;
    }
}
