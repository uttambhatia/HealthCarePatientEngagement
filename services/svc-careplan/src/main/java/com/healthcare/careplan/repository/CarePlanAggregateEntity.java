package com.healthcare.careplan.repository;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "careplan_aggregates")
public class CarePlanAggregateEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String patientId;

    @Column(nullable = false)
    private String goal;

    @Column(nullable = false)
    private String planStatus;

    @Column(nullable = false)
    private String ownerId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "careplan_tasks", joinColumns = @JoinColumn(name = "careplan_id"))
    @OrderColumn(name = "task_order")
    @Column(name = "task", nullable = false)
    private List<String> tasks = new ArrayList<>();

    @Column(nullable = false)
    private int version;

    protected CarePlanAggregateEntity() {
    }

    public CarePlanAggregateEntity(String id, String status, String patientId, String goal, String planStatus, String ownerId, List<String> tasks, int version) {
        this.id = id;
        this.status = status;
        this.patientId = patientId;
        this.goal = goal;
        this.planStatus = planStatus;
        this.ownerId = ownerId;
        this.tasks = tasks == null ? new ArrayList<>() : new ArrayList<>(tasks);
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

    public String getGoal() {
        return goal;
    }

    public String getPlanStatus() {
        return planStatus;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public List<String> getTasks() {
        return tasks;
    }

    public int getVersion() {
        return version;
    }
}