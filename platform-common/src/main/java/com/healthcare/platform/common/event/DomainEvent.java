package com.healthcare.platform.common.event;

public interface DomainEvent {
    String eventType();
    String aggregateId();
}
