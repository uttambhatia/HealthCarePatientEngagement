package com.healthcare.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {
    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("patients", route -> route.path("/api/patients/**").uri("http://localhost:8081"))
                .route("appointments", route -> route.path("/api/appointments/**").uri("http://localhost:8082"))
                .route("careplans", route -> route.path("/api/careplans/**").uri("http://localhost:8083"))
                .route("notifications", route -> route.path("/api/notifications/**").uri("http://localhost:8086"))
                .build();
    }
}
