package com.healthcare.careplan.repository;

import com.healthcare.careplan.domain.CarePlanAggregate;

import java.util.List;
import java.util.Optional;

public interface CarePlanRepository {
    CarePlanAggregate save(CarePlanAggregate aggregate);
    Optional<CarePlanAggregate> findById(String id);
    List<CarePlanAggregate> findAll();
    void deleteById(String id);
}
