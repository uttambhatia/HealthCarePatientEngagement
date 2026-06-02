package com.healthcare.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GatewayRoutesConfigTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void shouldRouteAppointmentBasePathToAppointmentService() {
        Route route = findRouteById("appointments");

        assertThat(route.getUri().toString()).isEqualTo("http://localhost:8082");
        assertThat(routeMatches(route, "/api/appointments")).isTrue();
        assertThat(routeMatches(route, "/api/appointments/apt-2001")).isTrue();
    }

    @Test
    void shouldRouteAvailableSlotsPathToAppointmentService() {
        Route route = findRouteById("appointments");

        assertThat(route.getUri().toString()).isEqualTo("http://localhost:8082");
        assertThat(routeMatches(route, "/api/appointments/available-slots?providerId=prov-44&date=2026-06-01")).isTrue();
        assertThat(route.getFilters().toString()).contains("StripPrefix");
    }

    private Route findRouteById(String id) {
        List<Route> routes = routeLocator.getRoutes().collectList().block();
        assertThat(routes).isNotNull();

        return routes.stream()
                .filter(route -> id.equals(route.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Route not found: " + id));
    }

    private boolean routeMatches(Route route, String rawPath) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get(rawPath).build()
        );
        Boolean matches = Mono.from(route.getPredicate().apply(exchange)).block();
        return Boolean.TRUE.equals(matches);
    }
}
