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
class GatewayDeviceIngestionForwardingIntegrationTest {

    private static HttpServer downstreamServer;
    private static final AtomicReference<String> lastMethod = new AtomicReference<>();
    private static final AtomicReference<String> lastPath = new AtomicReference<>();
    private static final AtomicReference<String> lastQuery = new AtomicReference<>();
    private static final AtomicReference<String> lastBody = new AtomicReference<>();

    @Autowired
    private WebTestClient webTestClient;

    @BeforeAll
    static void setUpDownstream() throws IOException {
        downstreamServer = HttpServer.create(new InetSocketAddress(8088), 0);

        downstreamServer.createContext("/devices/events", exchange -> {
            lastMethod.set(exchange.getRequestMethod());
            lastPath.set(exchange.getRequestURI().getPath());
            lastQuery.set(exchange.getRequestURI().getQuery());
            String body = readBody(exchange);
            lastBody.set(body);

            if (body.contains("\"deviceId\": \"dev-404\"")) {
                byte[] response = """
                        {"correlationId":"corr-gw-dvc-2","code":"DEVICE_NOT_REGISTERED","message":"Device is not registered: dev-404","timestamp":"2026-06-22T11:10:00Z"}
                        """.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(404, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
                return;
            }

            if (body.contains("\"deviceId\": \"dev-503\"")) {
                byte[] response = """
                        {"correlationId":"corr-gw-dvc-3","code":"TELEMETRY_FORWARDING_FAILED","message":"Telemetry forwarding failed for device ingestion id=ing-503","timestamp":"2026-06-22T11:11:00Z"}
                        """.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(503, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
                return;
            }

            byte[] response = """
                    {"correlationId":"corr-gw-dvc-1","data":{"id":"ing-1001","status":"INGESTED","deviceId":"dev-22","protocol":"MQTT","payload":"{\\\"spo2\\\":97}","receivedAt":"2026-05-31T10:07:00Z"}}
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
    void shouldForwardDeviceIngestionToDownstreamWithStrippedPrefix() {
        String payload = """
                {
                  "deviceId": "dev-22",
                  "protocol": "MQTT",
                                    "payload": "{\\\"spo2\\\":97}",
                  "receivedAt": "2026-05-31T10:07:00Z"
                }
                """;

        webTestClient.post()
                .uri("/api/devices/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data.id").isEqualTo("ing-1001")
                .jsonPath("$.data.status").isEqualTo("INGESTED");

        assertThat(lastMethod.get()).isEqualTo("POST");
        assertThat(lastPath.get()).isEqualTo("/devices/events");
        assertThat(lastQuery.get()).isNull();
        assertThat(lastBody.get()).contains("\"deviceId\": \"dev-22\"");
    }

    @Test
    void shouldPassThroughDeviceNotRegisteredErrorFromDownstream() {
        String payload = """
                {
                  "deviceId": "dev-404",
                  "protocol": "MQTT",
                                    "payload": "{\\\"hr\\\":72}",
                  "receivedAt": "2026-05-31T10:07:00Z"
                }
                """;

        webTestClient.post()
                .uri("/api/devices/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("DEVICE_NOT_REGISTERED")
                .jsonPath("$.message").isEqualTo("Device is not registered: dev-404");

        assertThat(lastMethod.get()).isEqualTo("POST");
        assertThat(lastPath.get()).isEqualTo("/devices/events");
        assertThat(lastBody.get()).contains("\"deviceId\": \"dev-404\"");
    }

    @Test
    void shouldPassThroughTelemetryForwardingFailureFromDownstream() {
        String payload = """
                {
                  "deviceId": "dev-503",
                  "protocol": "MQTT",
                                    "payload": "{\\\"glucose\\\":108}",
                  "receivedAt": "2026-05-31T10:07:00Z"
                }
                """;

        webTestClient.post()
                .uri("/api/devices/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.code").isEqualTo("TELEMETRY_FORWARDING_FAILED")
                .jsonPath("$.message").isEqualTo("Telemetry forwarding failed for device ingestion id=ing-503");

        assertThat(lastMethod.get()).isEqualTo("POST");
        assertThat(lastPath.get()).isEqualTo("/devices/events");
        assertThat(lastBody.get()).contains("\"deviceId\": \"dev-503\"");
    }

    private static String readBody(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        if (body.length == 0) {
            return "";
        }
        return new String(body, StandardCharsets.UTF_8);
    }
}
