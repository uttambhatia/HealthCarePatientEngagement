package com.healthcare.alertmanagement.repository;

import com.healthcare.alertmanagement.domain.ClinicalAlert;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryAlertRepository implements AlertRepository {
    private final Map<String, ClinicalAlert> store = new ConcurrentHashMap<>();

    @Override
    public ClinicalAlert save(ClinicalAlert aggregate) {
        store.put(aggregate.id(), aggregate);
        return aggregate;
    }

    @Override
    public Optional<ClinicalAlert> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<ClinicalAlert> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }
}
