package com.healthcare.careplan.config;

import com.healthcare.careplan.domain.CarePlanAggregate;
import com.healthcare.careplan.repository.CarePlanRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class CarePlanSeedInitializer implements ApplicationRunner {
    private final CarePlanRepository repository;
    private final boolean seedEnabled;

    public CarePlanSeedInitializer(
            CarePlanRepository repository,
            @Value("${platform.seed.careplanResponsibility.enabled:true}") boolean seedEnabled) {
        this.repository = repository;
        this.seedEnabled = seedEnabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!seedEnabled) {
            return;
        }

        String seedPatientId = "pat-seed-1001";
        if (repository.findLatestByPatientId(seedPatientId).isPresent()) {
            return;
        }

        repository.save(new CarePlanAggregate(
                UUID.randomUUID().toString(),
                "MANAGED",
                seedPatientId,
                "Telemetry monitoring ownership",
                "ACTIVE",
                "coord-seed-1001",
                List.of("Review telemetry trend daily"),
                1
        ));
    }
}
