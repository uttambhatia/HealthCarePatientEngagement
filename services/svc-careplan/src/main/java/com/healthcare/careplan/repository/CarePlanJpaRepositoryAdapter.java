package com.healthcare.careplan.repository;

import com.healthcare.careplan.domain.CarePlanAggregate;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class CarePlanJpaRepositoryAdapter implements CarePlanRepository {
    private final JpaCarePlanEntityRepository jpaRepository;

    public CarePlanJpaRepositoryAdapter(JpaCarePlanEntityRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public CarePlanAggregate save(CarePlanAggregate aggregate) {
        CarePlanAggregateEntity saved = jpaRepository.save(toEntity(aggregate));
        return toDomain(saved);
    }

    @Override
    public Optional<CarePlanAggregate> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<CarePlanAggregate> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<CarePlanAggregate> findLatestByPatientId(String patientId) {
        return jpaRepository.findTopByPatientIdOrderByVersionDesc(patientId).map(this::toDomain);
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    private CarePlanAggregateEntity toEntity(CarePlanAggregate aggregate) {
        return new CarePlanAggregateEntity(
                aggregate.id(),
                aggregate.status(),
                aggregate.patientId(),
                aggregate.goal(),
                aggregate.planStatus(),
                aggregate.ownerId(),
                aggregate.tasks(),
                aggregate.version()
        );
    }

    private CarePlanAggregate toDomain(CarePlanAggregateEntity entity) {
        return new CarePlanAggregate(
                entity.getId(),
                entity.getStatus(),
                entity.getPatientId(),
                entity.getGoal(),
                entity.getPlanStatus(),
                entity.getOwnerId(),
                entity.getTasks(),
                entity.getVersion()
        );
    }
}