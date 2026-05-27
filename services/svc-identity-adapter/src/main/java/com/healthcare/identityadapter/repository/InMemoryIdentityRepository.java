package com.healthcare.identityadapter.repository;

import com.healthcare.identityadapter.domain.IdentityAssertion;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryIdentityRepository implements IdentityRepository {
    private final Map<String, IdentityAssertion> store = new ConcurrentHashMap<>();

    @Override
    public IdentityAssertion save(IdentityAssertion aggregate) {
        store.put(aggregate.id(), aggregate);
        return aggregate;
    }

    @Override
    public Optional<IdentityAssertion> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<IdentityAssertion> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }
}
