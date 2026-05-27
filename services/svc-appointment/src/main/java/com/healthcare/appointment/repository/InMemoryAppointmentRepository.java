package com.healthcare.appointment.repository;

import com.healthcare.appointment.domain.AppointmentRecord;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryAppointmentRepository implements AppointmentRepository {
    private final Map<String, AppointmentRecord> store = new ConcurrentHashMap<>();

    @Override
    public AppointmentRecord save(AppointmentRecord aggregate) {
        store.put(aggregate.id(), aggregate);
        return aggregate;
    }

    @Override
    public Optional<AppointmentRecord> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<AppointmentRecord> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }
}
