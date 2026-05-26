package com.healthcare.eventmessaging.repository;

import com.healthcare.eventmessaging.domain.ServiceBusMessageRecord;

import java.util.List;
import java.util.Optional;

public interface ServiceBusMessageRepository {
    ServiceBusMessageRecord save(ServiceBusMessageRecord aggregate);
    Optional<ServiceBusMessageRecord> findById(String id);
    List<ServiceBusMessageRecord> findAll();
    void deleteById(String id);
}
