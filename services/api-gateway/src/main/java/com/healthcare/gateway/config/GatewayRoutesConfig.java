package com.healthcare.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {
    @Value("${platform.routes.patients-uri:http://svc-patient:8081}")
    private String patientsUri;

    @Value("${platform.routes.appointments-uri:http://svc-appointment:8082}")
    private String appointmentsUri;

    @Value("${platform.routes.careplans-uri:http://svc-careplan:8083}")
    private String careplansUri;

    @Value("${platform.routes.consents-uri:http://svc-consent:8084}")
    private String consentsUri;

    @Value("${platform.routes.medical-records-uri:http://svc-medical-record:8085}")
    private String medicalRecordsUri;

    @Value("${platform.routes.notifications-uri:http://svc-notification:8086}")
    private String notificationsUri;

    @Value("${platform.routes.telemetry-uri:http://svc-telemetry:8087}")
    private String telemetryUri;

    @Value("${platform.routes.device-events-uri:http://svc-device-ingestion:8088}")
    private String deviceEventsUri;

    @Value("${platform.routes.alerts-uri:http://svc-alert-management:8089}")
    private String alertsUri;

    @Value("${platform.routes.identity-assertions-uri:http://svc-identity-adapter:8090}")
    private String identityAssertionsUri;

    @Value("${platform.routes.servicebus-messages-uri:http://svc-event-messaging:8091}")
    private String servicebusMessagesUri;

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("patients", route -> route.path("/api/patients", "/api/patients/**").filters(filter -> filter.stripPrefix(1)).uri(patientsUri))
                .route("appointments", route -> route.path("/api/appointments", "/api/appointments/**").filters(filter -> filter.stripPrefix(1)).uri(appointmentsUri))
                .route("careplans", route -> route.path("/api/careplans", "/api/careplans/**").filters(filter -> filter.stripPrefix(1)).uri(careplansUri))
                .route("consents", route -> route.path("/api/consents", "/api/consents/**").filters(filter -> filter.stripPrefix(1)).uri(consentsUri))
                .route("medical-records", route -> route.path("/api/medical-records", "/api/medical-records/**").filters(filter -> filter.stripPrefix(1)).uri(medicalRecordsUri))
                .route("notifications", route -> route.path("/api/notifications", "/api/notifications/**").filters(filter -> filter.stripPrefix(1)).uri(notificationsUri))
                .route("telemetry", route -> route.path("/api/telemetry", "/api/telemetry/**").filters(filter -> filter.stripPrefix(1)).uri(telemetryUri))
                .route("device-events", route -> route.path("/api/devices/events", "/api/devices/events/**").filters(filter -> filter.stripPrefix(1)).uri(deviceEventsUri))
                .route("alerts", route -> route.path("/api/alerts", "/api/alerts/**").filters(filter -> filter.stripPrefix(1)).uri(alertsUri))
                .route("identity-assertions", route -> route.path("/api/identity/assertions", "/api/identity/assertions/**").filters(filter -> filter.stripPrefix(1)).uri(identityAssertionsUri))
                .route("servicebus-messages", route -> route.path("/api/servicebus/messages", "/api/servicebus/messages/**").filters(filter -> filter.stripPrefix(1)).uri(servicebusMessagesUri))
                .build();
    }
}
