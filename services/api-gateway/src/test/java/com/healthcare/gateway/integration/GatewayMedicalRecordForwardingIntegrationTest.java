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
class GatewayMedicalRecordForwardingIntegrationTest {

    private static HttpServer downstreamServer;
    private static final AtomicReference<String> lastMethod = new AtomicReference<>();
    private static final AtomicReference<String> lastPath = new AtomicReference<>();
    private static final AtomicReference<String> lastQuery = new AtomicReference<>();
    private static final AtomicReference<String> lastBody = new AtomicReference<>();

    @Autowired
    private WebTestClient webTestClient;

    @BeforeAll
    static void setUpDownstream() throws IOException {
        downstreamServer = HttpServer.create(new InetSocketAddress(8085), 0);

        downstreamServer.createContext("/medical-records/mr-901", exchange -> {
            lastMethod.set(exchange.getRequestMethod());
            lastPath.set(exchange.getRequestURI().getPath());
            lastQuery.set(exchange.getRequestURI().getQuery());
            lastBody.set(readBody(exchange));

            byte[] response = """
                    {"correlationId":"corr-gw-mr-1","data":{"id":"mr-901","status":"SYNCED","patientId":"pat-1001","fhirResourceType":"Condition","resourceReference":"Condition/cond-332","summary":"Follow-up diagnosis updated after consultation","version":2}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        downstreamServer.createContext("/medical-records/mr-902", exchange -> {
            lastMethod.set(exchange.getRequestMethod());
            lastPath.set(exchange.getRequestURI().getPath());
            lastQuery.set(exchange.getRequestURI().getQuery());
            lastBody.set(readBody(exchange));

            byte[] response = """
                    {"correlationId":"corr-gw-mr-2","code":"VERSION_CONFLICT","message":"Medical record version conflict for id=mr-902 expectedVersion=5 actualVersion=1","timestamp":"2026-06-21T11:00:00Z"}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(409, response.length);
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
    void shouldForwardMedicalRecordUpdateToDownstreamWithStrippedPrefix() {
        String payload = """
                {
                  "fhirResourceType": "Condition",
                  "resourceReference": "Condition/cond-332",
                  "summary": "Follow-up diagnosis updated after consultation",
                  "expectedVersion": 1
                }
                """;

        webTestClient.put()
                .uri("/api/medical-records/mr-901")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.id").isEqualTo("mr-901")
                .jsonPath("$.data.version").isEqualTo(2)
                .jsonPath("$.data.fhirResourceType").isEqualTo("Condition");

        assertThat(lastMethod.get()).isEqualTo("PUT");
        assertThat(lastPath.get()).isEqualTo("/medical-records/mr-901");
        assertThat(lastBody.get()).contains("expectedVersion");
        assertThat(lastBody.get()).contains("Follow-up diagnosis updated after consultation");
    }

    @Test
    void shouldPassThroughMedicalRecordVersionConflict() {
        String payload = """
                {
                  "fhirResourceType": "Condition",
                  "resourceReference": "Condition/cond-332",
                  "summary": "Stale update attempt",
                  "expectedVersion": 5
                }
                """;

        webTestClient.put()
                .uri("/api/medical-records/mr-902")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("VERSION_CONFLICT")
                .jsonPath("$.message").isEqualTo("Medical record version conflict for id=mr-902 expectedVersion=5 actualVersion=1");

        assertThat(lastMethod.get()).isEqualTo("PUT");
        assertThat(lastPath.get()).isEqualTo("/medical-records/mr-902");
        assertThat(lastBody.get()).contains("Stale update attempt");
    }

    private static String readBody(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        if (body.length == 0) {
            return "";
        }
        return new String(body, StandardCharsets.UTF_8);
    }
}
