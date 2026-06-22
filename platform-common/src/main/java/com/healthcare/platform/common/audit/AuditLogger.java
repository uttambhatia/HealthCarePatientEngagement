package com.healthcare.platform.common.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Lightweight audit logger that writes structured audit entries to a dedicated
 * audit SLF4J logger channel. Downstream log aggregation (Azure Monitor / SIEM)
 * can filter on the "AUDIT" marker to build the compliance trail.
 *
 * Services that require durable DB-backed audit tables should extend this with
 * their own JPA entity and call super.log() as well.
 */
@Component
public class AuditLogger {
    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("com.healthcare.platform.audit");

    public void log(AuditEntry entry) {
        AUDIT_LOG.info(entry.toLogString());
    }

    public void log(String actor, String action, String aggregateId, String correlationId) {
        log(new AuditEntry(actor, action, aggregateId, correlationId));
    }
}
