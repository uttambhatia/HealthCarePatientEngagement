package com.healthcare.gateway.repository;

import com.healthcare.gateway.domain.ModuleSummary;

import java.util.List;

public interface GatewayRouteRepository {
    List<ModuleSummary> findAll();
}
