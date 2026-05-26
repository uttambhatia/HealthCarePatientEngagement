package com.healthcare.identityadapter.event;

import com.healthcare.platform.common.event.DomainEvent;

public record IdentityValidatedEvent(
        String aggregateId,
String subject,
String tenantId,
String role,
String tokenId
) implements DomainEvent {
    @Override
    public String eventType() {
        return "IdentityValidatedEvent";
    }
}
