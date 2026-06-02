package com.healthcare.gateway.integration;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayNotificationForwardingIntegrationTest {

    private static HttpServer downstreamServer;
    private static final AtomicReference<String> lastMethod = new AtomicReference<>();
    private static final AtomicReference<String> lastPath = new AtomicReference<>();
    private static final AtomicReference<String> lastQuery = new AtomicReference<>();
    private static final AtomicReference<String> lastBody = new AtomicReference<>();

    @Autowired
    private WebTestClient webTestClient;

    @BeforeAll
    static void setUpDownstream() throws IOException {
        downstreamServer = HttpServer.create(new InetSocketAddress(8086), 0);

        downstreamServer.createContext("/notifications", exchange -> {
            lastMethod.set(exchange.getRequestMethod());
            lastPath.set(exchange.getRequestURI().getPath());
            lastQuery.set(exchange.getRequestURI().getQuery());
            String body = readBody(exchange);
            lastBody.set(body);

            boolean forceFailure = body.contains("\"templateId\": \"force-failure\"");
            if (forceFailure) {
                byte[] response = """
                        {"correlationId":"corr-gw-not-2","code":"NOTIFICATION_DELIVERY_FAILED","message":"Notification delivery failed for id=not-502 reason=ACS unavailable","timestamp":"2026-06-21T11:10:00Z"}
                        """.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(503, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
                return;
            }

            byte[] response = """
                    {"correlationId":"corr-gw-not-1","data":{"id":"not-501","status":"DELIVERED","recipientId":"pat-1001","channel":"SMS","templateId":"appt-reminder-v1","message":"Your appointment is tomorrow at 09:30.","deliveryAttempts":1,"lastError":null}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        downstreamServer.start();
    }

    @AfterAll
    static void tearDownDownstream() {
        if (downstreamServer != null) {
            downstreamServer.stop(0);
        }
    }

    @BeforeEach
    void resetCapture() {
        lastMethod.set(null);
        lastPath.set(null);
        lastQuery.set(null);
        lastBody.set(null);
    }

    @Test
    void shouldForwardNotificationDispatchToDownstreamWithStrippedPrefix() {
        String payload = """
                {
                  "recipientId": "pat-1001",
                  "channel": "SMS",
                  "templateId": "appt-reminder-v1",
                  "message": "Your appointment is tomorrow at 09:30."
                }
                """;

        webTestClient.post()
                .uri("/api/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data.id").isEqualTo("not-501")
                .jsonPath("$.data.status").isEqualTo("DELIVERED")
                .jsonPath("$.data.deliveryAttempts").isEqualTo(1);

        assertThat(lastMethod.get()).isEqualTo("POST");
        assertThat(lastPath.get()).isEqualTo("/notifications");
        assertThat(lastQuery.get()).isNull();
        assertThat(lastBody.get()).contains("\"channel\": \"SMS\"");
    }

    @Test
    void shouldPassThroughNotificationDeliveryFailureFromDownstream() {
        String payload = """
                {
                  "recipientId": "pat-1002",
                  "channel": "EMAIL",
                  "templateId": "force-failure",
                  "message": "Delivery failure passthrough test"
                }
                """;

        webTestClient.post()
                .uri("/api/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.code").isEqualTo("NOTIFICATION_DELIVERY_FAILED")
                .jsonPath("$.message").isEqualTo("Notification delivery failed for id=not-502 reason=ACS unavailable");

        assertThat(lastMethod.get()).isEqualTo("POST");
        assertThat(lastPath.get()).isEqualTo("/notifications");
        assertThat(lastBody.get()).contains("\"templateId\": \"force-failure\"");
    }

    private static String readBody(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        if (body.length == 0) {
            return "";
        }
        return new String(body, StandardCharsets.UTF_8);
    }
}
