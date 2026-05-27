package com.healthcare.gateway.event;

import com.healthcare.platform.common.event.DomainEvent;

public record GatewayAuditEvent(String aggregateId, String action) implements DomainEvent {
    @Override
    public String eventType() {
        return "GatewayAuditEvent";
    }
}
