package com.healthcare.careplan.repository;

import com.healthcare.careplan.domain.CarePlanAggregate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryCarePlanRepository implements CarePlanRepository {
    private final Map<String, CarePlanAggregate> store = new ConcurrentHashMap<>();

    @Override
    public CarePlanAggregate save(CarePlanAggregate aggregate) {
        store.put(aggregate.id(), aggregate);
        return aggregate;
    }

    @Override
    public Optional<CarePlanAggregate> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<CarePlanAggregate> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }
}
