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
class GatewayEventMessagingForwardingIntegrationTest {

    private static HttpServer downstreamServer;
    private static final AtomicReference<String> lastMethod = new AtomicReference<>();
    private static final AtomicReference<String> lastPath = new AtomicReference<>();
    private static final AtomicReference<String> lastQuery = new AtomicReference<>();
    private static final AtomicReference<String> lastBody = new AtomicReference<>();

    @Autowired
    private WebTestClient webTestClient;

    @BeforeAll
    static void setUpDownstream() throws IOException {
        downstreamServer = HttpServer.create(new InetSocketAddress(8091), 0);

        downstreamServer.createContext("/servicebus/messages", exchange -> {
            lastMethod.set(exchange.getRequestMethod());
            lastPath.set(exchange.getRequestURI().getPath());
            lastQuery.set(exchange.getRequestURI().getQuery());
            String body = readBody(exchange);
            lastBody.set(body);

            if (body.contains("\"eventName\": \"MonitorFailure\"")) {
                byte[] response = """
                        {"correlationId":"corr-gw-esb-2","code":"MONITORING_DISPATCH_FAILED","message":"Monitoring dispatch failed for message id=msg-602","timestamp":"2026-06-22T13:00:00Z"}
                        """.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(503, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
                return;
            }

            byte[] response = """
                    {"correlationId":"corr-gw-esb-1","data":{"id":"msg-601","status":"QUEUED","channel":"care-events","eventName":"AppointmentBooked","payload":"{\\\"appointmentId\\\":\\\"apt-2001\\\"}","messageType":"DOMAIN_EVENT","recordedAt":"2026-06-22T12:59:00Z","integrityHash":"abc123hash","anomalyReason":null}}
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
    void shouldForwardServiceBusMessageToDownstreamWithStrippedPrefix() {
        String payload = """
                {
                  "channel": "care-events",
                  "eventName": "AppointmentBooked",
                  "payload": "{\\\"appointmentId\\\":\\\"apt-2001\\\"}",
                  "messageType": "DOMAIN_EVENT"
                }
                """;

        webTestClient.post()
                .uri("/api/servicebus/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data.id").isEqualTo("msg-601")
                .jsonPath("$.data.status").isEqualTo("QUEUED");

        assertThat(lastMethod.get()).isEqualTo("POST");
        assertThat(lastPath.get()).isEqualTo("/servicebus/messages");
        assertThat(lastQuery.get()).isNull();
        assertThat(lastBody.get()).contains("\"eventName\": \"AppointmentBooked\"");
    }

    @Test
    void shouldPassThroughMonitoringDispatchFailureFromDownstream() {
        String payload = """
                {
                  "channel": "care-events",
                  "eventName": "MonitorFailure",
                  "payload": "ERROR monitoring backend unavailable",
                  "messageType": "DOMAIN_EVENT"
                }
                """;

        webTestClient.post()
                .uri("/api/servicebus/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.code").isEqualTo("MONITORING_DISPATCH_FAILED")
                .jsonPath("$.message").isEqualTo("Monitoring dispatch failed for message id=msg-602");

        assertThat(lastMethod.get()).isEqualTo("POST");
        assertThat(lastPath.get()).isEqualTo("/servicebus/messages");
        assertThat(lastBody.get()).contains("\"eventName\": \"MonitorFailure\"");
    }

    private static String readBody(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        if (body.length == 0) {
            return "";
        }
        return new String(body, StandardCharsets.UTF_8);
    }
}
