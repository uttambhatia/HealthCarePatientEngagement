package com.healthcare.gateway.service;

import com.healthcare.gateway.dto.DashboardResponse;
import com.healthcare.gateway.dto.RouteResponse;
import com.healthcare.gateway.repository.GatewayRouteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BffDashboardServiceImpl implements BffDashboardService {
    private final GatewayRouteRepository repository;

    public BffDashboardServiceImpl(GatewayRouteRepository repository) {
        this.repository = repository;
    }

    @Override
    public DashboardResponse getDashboard() {
        List<RouteResponse> routes = repository.findAll().stream()
                .map(route -> new RouteResponse(route.id(), route.title(), route.route()))
                .toList();
        return new DashboardResponse(List.of("PATIENT", "DOCTOR", "ADMIN", "COORDINATOR"), routes);
    }
}
