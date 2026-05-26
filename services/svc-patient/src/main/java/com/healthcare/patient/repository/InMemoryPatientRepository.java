package com.healthcare.patient.repository;

import com.healthcare.patient.domain.PatientProfile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPatientRepository implements PatientRepository {
    private final Map<String, PatientProfile> store = new ConcurrentHashMap<>();

    @Override
    public PatientProfile save(PatientProfile aggregate) {
        store.put(aggregate.id(), aggregate);
        return aggregate;
    }

    @Override
    public Optional<PatientProfile> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<PatientProfile> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }
}
