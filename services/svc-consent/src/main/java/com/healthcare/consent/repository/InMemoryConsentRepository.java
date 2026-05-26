package com.healthcare.consent.repository;

import com.healthcare.consent.domain.ConsentRecord;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryConsentRepository implements ConsentRepository {
    private final Map<String, ConsentRecord> store = new ConcurrentHashMap<>();

    @Override
    public ConsentRecord save(ConsentRecord aggregate) {
        store.put(aggregate.id(), aggregate);
        return aggregate;
    }

    @Override
    public Optional<ConsentRecord> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<ConsentRecord> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }
}
