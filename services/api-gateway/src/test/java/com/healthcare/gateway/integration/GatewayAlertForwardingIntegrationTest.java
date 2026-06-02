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
class GatewayAlertForwardingIntegrationTest {

    private static HttpServer downstreamServer;
    private static final AtomicReference<String> lastMethod = new AtomicReference<>();
    private static final AtomicReference<String> lastPath = new AtomicReference<>();
    private static final AtomicReference<String> lastQuery = new AtomicReference<>();
    private static final AtomicReference<String> lastBody = new AtomicReference<>();

    @Autowired
    private WebTestClient webTestClient;

    @BeforeAll
    static void setUpDownstream() throws IOException {
        downstreamServer = HttpServer.create(new InetSocketAddress(8089), 0);

        downstreamServer.createContext("/alerts", exchange -> {
            lastMethod.set(exchange.getRequestMethod());
            lastPath.set(exchange.getRequestURI().getPath());
            lastQuery.set(exchange.getRequestURI().getQuery());
            String body = readBody(exchange);
            lastBody.set(body);

            if (body.contains("\"patientId\": \"pat-503\"")) {
                byte[] response = """
                        {"correlationId":"corr-gw-alt-2","code":"ALERT_ESCALATION_FAILED","message":"Alert escalation failed for alertId=alt-702","timestamp":"2026-06-22T12:40:00Z"}
                        """.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(503, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
                return;
            }

            byte[] response = """
                    {"correlationId":"corr-gw-alt-1","data":{"id":"alt-701","status":"OPEN","patientId":"pat-1001","severity":"HIGH","triggerType":"HEART_RATE","summary":"Heart rate threshold breached"}}
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
    void shouldForwardAlertTriggerToDownstreamWithStrippedPrefix() {
        String payload = """
                {
                  "patientId": "pat-1001",
                  "severity": "HIGH",
                  "triggerType": "HEART_RATE",
                  "metricValue": "124",
                  "summary": "Heart rate threshold breached"
                }
                """;

        webTestClient.post()
                .uri("/api/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data.id").isEqualTo("alt-701")
                .jsonPath("$.data.status").isEqualTo("OPEN");

        assertThat(lastMethod.get()).isEqualTo("POST");
        assertThat(lastPath.get()).isEqualTo("/alerts");
        assertThat(lastQuery.get()).isNull();
        assertThat(lastBody.get()).contains("\"triggerType\": \"HEART_RATE\"");
    }

    @Test
    void shouldPassThroughAlertEscalationFailureFromDownstream() {
        String payload = """
                {
                  "patientId": "pat-503",
                  "severity": "HIGH",
                  "triggerType": "HEART_RATE",
                  "metricValue": "140",
                  "summary": "Escalation failure test"
                }
                """;

        webTestClient.post()
                .uri("/api/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.code").isEqualTo("ALERT_ESCALATION_FAILED")
                .jsonPath("$.message").isEqualTo("Alert escalation failed for alertId=alt-702");

        assertThat(lastMethod.get()).isEqualTo("POST");
        assertThat(lastPath.get()).isEqualTo("/alerts");
        assertThat(lastBody.get()).contains("\"patientId\": \"pat-503\"");
    }

    private static String readBody(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        if (body.length == 0) {
            return "";
        }
        return new String(body, StandardCharsets.UTF_8);
    }
}
