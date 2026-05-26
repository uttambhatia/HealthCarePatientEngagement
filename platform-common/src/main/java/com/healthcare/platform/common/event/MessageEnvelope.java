package com.healthcare.platform.common.event;

import java.time.OffsetDateTime;

public record MessageEnvelope<T>(String correlationId, String eventName, OffsetDateTime occurredAt, T payload) {
}
