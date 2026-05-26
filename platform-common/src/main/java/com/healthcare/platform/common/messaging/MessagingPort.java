package com.healthcare.platform.common.messaging;

import com.healthcare.platform.common.event.DomainEvent;

public interface MessagingPort {
    void publish(String channel, String correlationId, DomainEvent event);
}
