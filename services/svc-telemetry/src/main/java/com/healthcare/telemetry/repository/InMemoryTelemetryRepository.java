package com.healthcare.telemetry.repository;

import com.healthcare.telemetry.domain.TelemetryReading;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryTelemetryRepository implements TelemetryRepository {
    private final Map<String, TelemetryReading> store = new ConcurrentHashMap<>();

    @Override
    public TelemetryReading save(TelemetryReading aggregate) {
        store.put(aggregate.id(), aggregate);
        return aggregate;
    }

    @Override
    public Optional<TelemetryReading> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<TelemetryReading> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }
}
