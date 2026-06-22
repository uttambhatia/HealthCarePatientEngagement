package com.healthcare.platform.common.audit;

import java.time.OffsetDateTime;

/**
 * Immutable structured audit record capturing who did what on which aggregate.
 * Services persist this to their own audit_logs table via {@link AuditLogger}.
 */
public record AuditEntry(
        String actor,
        String action,
        String aggregateId,
        String correlationId,
        OffsetDateTime occurredAt
) {
    public AuditEntry(String actor, String action, String aggregateId, String correlationId) {
        this(actor, action, aggregateId, correlationId, OffsetDateTime.now());
    }

    public String toLogString() {
        return "AUDIT actor=%s action=%s aggregateId=%s correlationId=%s occurredAt=%s"
                .formatted(actor, action, aggregateId, correlationId, occurredAt);
    }
}
