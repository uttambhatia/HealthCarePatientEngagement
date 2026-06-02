package com.healthcare.notification.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_jobs")
public class NotificationJobEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String recipientId;

    @Column(nullable = false)
    private String channel;

    @Column(nullable = false)
    private String templateId;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private int deliveryAttempts;

    @Column(length = 1000)
    private String lastError;

    protected NotificationJobEntity() {
    }

    public NotificationJobEntity(String id, String status, String recipientId, String channel, String templateId, String message, int deliveryAttempts, String lastError) {
        this.id = id;
        this.status = status;
        this.recipientId = recipientId;
        this.channel = channel;
        this.templateId = templateId;
        this.message = message;
        this.deliveryAttempts = deliveryAttempts;
        this.lastError = lastError;
    }

    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public String getChannel() {
        return channel;
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getMessage() {
        return message;
    }

    public int getDeliveryAttempts() {
        return deliveryAttempts;
    }

    public String getLastError() {
        return lastError;
    }
}