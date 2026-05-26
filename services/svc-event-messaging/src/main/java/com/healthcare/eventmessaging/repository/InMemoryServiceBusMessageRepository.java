package com.healthcare.eventmessaging.repository;

import com.healthcare.eventmessaging.domain.ServiceBusMessageRecord;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryServiceBusMessageRepository implements ServiceBusMessageRepository {
    private final Map<String, ServiceBusMessageRecord> store = new ConcurrentHashMap<>();

    @Override
    public ServiceBusMessageRecord save(ServiceBusMessageRecord aggregate) {
        store.put(aggregate.id(), aggregate);
        return aggregate;
    }

    @Override
    public Optional<ServiceBusMessageRecord> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<ServiceBusMessageRecord> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }
}
