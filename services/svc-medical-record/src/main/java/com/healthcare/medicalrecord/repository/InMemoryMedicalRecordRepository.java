package com.healthcare.medicalrecord.repository;

import com.healthcare.medicalrecord.domain.MedicalRecordSnapshot;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryMedicalRecordRepository implements MedicalRecordRepository {
    private final Map<String, MedicalRecordSnapshot> store = new ConcurrentHashMap<>();

    @Override
    public MedicalRecordSnapshot save(MedicalRecordSnapshot aggregate) {
        store.put(aggregate.id(), aggregate);
        return aggregate;
    }

    @Override
    public Optional<MedicalRecordSnapshot> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<MedicalRecordSnapshot> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }
}
