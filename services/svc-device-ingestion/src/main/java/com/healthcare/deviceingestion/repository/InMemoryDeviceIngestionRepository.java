package com.healthcare.deviceingestion.repository;

import com.healthcare.deviceingestion.domain.DeviceMessage;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryDeviceIngestionRepository implements DeviceIngestionRepository {
    private final Map<String, DeviceMessage> store = new ConcurrentHashMap<>();

    @Override
    public DeviceMessage save(DeviceMessage aggregate) {
        store.put(aggregate.id(), aggregate);
        return aggregate;
    }

    @Override
    public Optional<DeviceMessage> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<DeviceMessage> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }
}
