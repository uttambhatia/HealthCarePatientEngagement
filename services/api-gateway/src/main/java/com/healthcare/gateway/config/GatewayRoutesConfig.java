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
                .route("patients", route -> route.path("/api/patients", "/api/patients/**").filters(filter -> filter.stripPrefix(1)).uri("http://localhost:8081"))
                .route("appointments", route -> route.path("/api/appointments", "/api/appointments/**").filters(filter -> filter.stripPrefix(1)).uri("http://localhost:8082"))
                .route("careplans", route -> route.path("/api/careplans", "/api/careplans/**").filters(filter -> filter.stripPrefix(1)).uri("http://localhost:8083"))
                .route("consents", route -> route.path("/api/consents", "/api/consents/**").filters(filter -> filter.stripPrefix(1)).uri("http://localhost:8084"))
                .route("medical-records", route -> route.path("/api/medical-records", "/api/medical-records/**").filters(filter -> filter.stripPrefix(1)).uri("http://localhost:8085"))
                .route("notifications", route -> route.path("/api/notifications", "/api/notifications/**").filters(filter -> filter.stripPrefix(1)).uri("http://localhost:8086"))
                .route("telemetry", route -> route.path("/api/telemetry", "/api/telemetry/**").filters(filter -> filter.stripPrefix(1)).uri("http://localhost:8087"))
                .route("device-events", route -> route.path("/api/devices/events", "/api/devices/events/**").filters(filter -> filter.stripPrefix(1)).uri("http://localhost:8088"))
                .route("alerts", route -> route.path("/api/alerts", "/api/alerts/**").filters(filter -> filter.stripPrefix(1)).uri("http://localhost:8089"))
                .route("identity-assertions", route -> route.path("/api/identity/assertions", "/api/identity/assertions/**").filters(filter -> filter.stripPrefix(1)).uri("http://localhost:8090"))
                .route("servicebus-messages", route -> route.path("/api/servicebus/messages", "/api/servicebus/messages/**").filters(filter -> filter.stripPrefix(1)).uri("http://localhost:8091"))
                .build();
    }
}
