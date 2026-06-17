package com.healthcare.gateway.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "platform.security.enabled=true",
            "platform.security.oauth2.issuer=https://login.microsoftonline.com/test-tenant/v2.0",
            "platform.security.oauth2.audience=api://healthcare-gateway"
        }
)
@AutoConfigureWebTestClient
class GatewaySecurityRbacIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldRejectApiRequestWithoutToken() {
        webTestClient.get()
                .uri("/api/appointments")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldForbidPatientRoleOnDeviceIngestionApi() {
        clientWithRoleAndCsrf("PATIENT")
                .post()
                .uri("/api/devices/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void shouldAllowDeviceIdentityOnDeviceIngestionApi() {
        assertNotUnauthorizedOrForbidden(
                clientWithRoleAndCsrf("DEVICE_IDENTITY")
                        .post()
                        .uri("/api/devices/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("{}")
                        .exchange()
                        .returnResult(String.class)
                        .getStatus()
        );
    }

    @Test
    void shouldForbidPatientRoleOnServiceBusApi() {
        clientWithRoleAndCsrf("PATIENT")
                .post()
                .uri("/api/servicebus/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void shouldAllowSystemIntegrationOnServiceBusApi() {
        assertNotUnauthorizedOrForbidden(
                clientWithRoleAndCsrf("SYSTEM_INTEGRATION")
                        .post()
                        .uri("/api/servicebus/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("{}")
                        .exchange()
                        .returnResult(String.class)
                        .getStatus()
        );
    }

    @Test
    void shouldForbidSystemIntegrationOnAppointmentsApi() {
        clientWithRoleAndCsrf("SYSTEM_INTEGRATION")
                .post()
                .uri("/api/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isForbidden();
    }

        private WebTestClient clientWithRoleAndCsrf(String role) {
        return webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt()
                .authorities(new SimpleGrantedAuthority("ROLE_" + role))
            .jwt(jwt -> jwt.claim("roles", List.of(role))))
            .mutateWith(SecurityMockServerConfigurers.csrf());
    }

    private void assertNotUnauthorizedOrForbidden(HttpStatusCode status) {
        if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
            throw new AssertionError("Expected non-authz failure status but got " + status.value());
        }
    }
}
