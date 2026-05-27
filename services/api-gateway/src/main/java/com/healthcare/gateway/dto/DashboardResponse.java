package com.healthcare.gateway.dto;

import java.util.List;

public record DashboardResponse(List<String> roles, List<RouteResponse> routes) {
}
