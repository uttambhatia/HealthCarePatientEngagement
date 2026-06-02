package com.healthcare.eventmessaging.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "servicebus_messages")
public class ServiceBusMessageEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String channel;

    @Column(nullable = false)
    private String eventName;

    @Column(nullable = false)
    private String payload;

    @Column(nullable = false)
    private String messageType;

    @Column(nullable = false)
    private String recordedAt;

    @Column(nullable = false, length = 128)
    private String integrityHash;

    @Column
    private String anomalyReason;

    protected ServiceBusMessageEntity() {
    }

    public ServiceBusMessageEntity(String id, String status, String channel, String eventName, String payload, String messageType, String recordedAt, String integrityHash, String anomalyReason) {
        this.id = id;
        this.status = status;
        this.channel = channel;
        this.eventName = eventName;
        this.payload = payload;
        this.messageType = messageType;
        this.recordedAt = recordedAt;
        this.integrityHash = integrityHash;
        this.anomalyReason = anomalyReason;
    }

    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public String getChannel() {
        return channel;
    }

    public String getEventName() {
        return eventName;
    }

    public String getPayload() {
        return payload;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getRecordedAt() {
        return recordedAt;
    }

    public String getIntegrityHash() {
        return integrityHash;
    }

    public String getAnomalyReason() {
        return anomalyReason;
    }
}
