package com.healthcare.gateway.repository;

import com.healthcare.gateway.domain.ModuleSummary;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class InMemoryGatewayRouteRepository implements GatewayRouteRepository {
    @Override
    public List<ModuleSummary> findAll() {
        return List.of(
                new ModuleSummary("patients", "Patient Service", "/api/patients", "/api/patients"),
                new ModuleSummary("appointments", "Appointment Service", "/api/appointments", "/api/appointments"),
                new ModuleSummary("careplans", "Care Plan Service", "/api/careplans", "/api/careplans")
        );
    }
}
