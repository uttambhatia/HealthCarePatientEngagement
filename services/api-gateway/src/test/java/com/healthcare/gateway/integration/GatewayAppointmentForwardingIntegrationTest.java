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
class GatewayAppointmentForwardingIntegrationTest {

    private static HttpServer downstreamServer;
    private static final AtomicReference<String> lastMethod = new AtomicReference<>();
    private static final AtomicReference<String> lastPath = new AtomicReference<>();
    private static final AtomicReference<String> lastQuery = new AtomicReference<>();
    private static final AtomicReference<String> lastBody = new AtomicReference<>();

    @Autowired
    private WebTestClient webTestClient;

    @BeforeAll
    static void setUpDownstream() throws IOException {
        downstreamServer = HttpServer.create(new InetSocketAddress(8082), 0);

        downstreamServer.createContext("/appointments/available-slots", exchange -> {
            lastMethod.set(exchange.getRequestMethod());
            lastPath.set(exchange.getRequestURI().getPath());
            lastQuery.set(exchange.getRequestURI().getQuery());
            lastBody.set(readBody(exchange));

            byte[] response = """
                    {"correlationId":"corr-gw-2","data":{"providerId":"prov-44","date":"2026-06-15","availableSlots":["2026-06-15T09:00:00Z","2026-06-15T10:00:00Z"]}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        downstreamServer.createContext("/appointments", exchange -> {
            lastMethod.set(exchange.getRequestMethod());
            lastPath.set(exchange.getRequestURI().getPath());
            lastQuery.set(exchange.getRequestURI().getQuery());
            lastBody.set(readBody(exchange));

            byte[] response = """
                    {"correlationId":"corr-gw-1","data":{"id":"apt-2001","status":"BOOKED","patientId":"pat-1001","providerId":"prov-44","scheduledAt":"2026-06-15T09:30:00Z","channel":"VIDEO"}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        downstreamServer.createContext("/appointments/apt-2001/teleconsult/start", exchange -> {
            lastMethod.set(exchange.getRequestMethod());
            lastPath.set(exchange.getRequestURI().getPath());
            lastQuery.set(exchange.getRequestURI().getQuery());
            lastBody.set(readBody(exchange));

            byte[] response = """
                    {"correlationId":"corr-gw-3","data":{"sessionId":"tcs-5001","appointmentId":"apt-2001","status":"INITIATED","doctorJoinUrl":"https://teleconsult.healthcare.local/session/tcs-5001?role=DOCTOR","patientJoinUrl":"https://teleconsult.healthcare.local/session/tcs-5001?role=PATIENT","startedAt":"2026-06-20T10:25:00Z","joinedAt":null,"completedAt":null,"consultationNotes":null,"interactionLogs":["2026-06-20T10:25:00Z | Teleconsultation initiated by doctor"]}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        downstreamServer.createContext("/appointments/apt-2001/teleconsult/complete", exchange -> {
            lastMethod.set(exchange.getRequestMethod());
            lastPath.set(exchange.getRequestURI().getPath());
            lastQuery.set(exchange.getRequestURI().getQuery());
            lastBody.set(readBody(exchange));

            byte[] response = """
                    {"correlationId":"corr-gw-4","data":{"sessionId":"tcs-5001","appointmentId":"apt-2001","status":"COMPLETED","doctorJoinUrl":"https://teleconsult.healthcare.local/session/tcs-5001?role=DOCTOR","patientJoinUrl":"https://teleconsult.healthcare.local/session/tcs-5001?role=PATIENT","startedAt":"2026-06-20T10:25:00Z","joinedAt":"2026-06-20T10:30:30Z","completedAt":"2026-06-20T11:05:10Z","consultationNotes":"Consultation completed and medication plan reviewed.","interactionLogs":["2026-06-20T10:25:00Z | Teleconsultation initiated by doctor","2026-06-20T10:30:30Z | Patient joined teleconsultation session","2026-06-20T11:05:10Z | Doctor completed teleconsultation and notes were recorded"]}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        downstreamServer.createContext("/appointments/apt-5001/teleconsult/complete", exchange -> {
            lastMethod.set(exchange.getRequestMethod());
            lastPath.set(exchange.getRequestURI().getPath());
            lastQuery.set(exchange.getRequestURI().getQuery());
            lastBody.set(readBody(exchange));

            byte[] response = """
                    {"correlationId":"corr-gw-5","code":"TELECONSULTATION_NETWORK_FAILURE","message":"Medical-record service unavailable","timestamp":"2026-06-20T11:05:10Z"}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(503, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        downstreamServer.createContext("/appointments/apt-4041/teleconsult/join", exchange -> {
            lastMethod.set(exchange.getRequestMethod());
            lastPath.set(exchange.getRequestURI().getPath());
            lastQuery.set(exchange.getRequestURI().getQuery());
            lastBody.set(readBody(exchange));

            byte[] response = """
                    {"correlationId":"corr-gw-6","code":"TELECONSULTATION_NOT_FOUND","message":"Teleconsultation session not found for appointmentId=apt-4041","timestamp":"2026-06-20T11:10:00Z"}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(404, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        downstreamServer.createContext("/appointments/apt-4091/teleconsult/complete", exchange -> {
            lastMethod.set(exchange.getRequestMethod());
            lastPath.set(exchange.getRequestURI().getPath());
            lastQuery.set(exchange.getRequestURI().getQuery());
            lastBody.set(readBody(exchange));

            byte[] response = """
                    {"correlationId":"corr-gw-7","code":"APPOINTMENT_NOT_ELIGIBLE","message":"appointmentId=apt-4091 not eligible for teleconsultation; teleconsultation already completed","timestamp":"2026-06-20T11:15:00Z"}
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
    void shouldForwardAppointmentBookingToDownstreamWithStrippedPrefix() {
        String payload = """
                {
                  "patientId": "pat-1001",
                  "providerId": "prov-44",
                  "scheduledAt": "2026-06-15T09:30:00Z",
                  "channel": "VIDEO"
                }
                """;

        webTestClient.post()
                .uri("/api/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data.id").isEqualTo("apt-2001")
                .jsonPath("$.data.status").isEqualTo("BOOKED");

        assertThat(lastMethod.get()).isEqualTo("POST");
        assertThat(lastPath.get()).isEqualTo("/appointments");
        assertThat(lastBody.get()).contains("\"patientId\": \"pat-1001\"");
    }

    @Test
    void shouldForwardAvailableSlotsQueryToDownstreamWithStrippedPrefix() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/appointments/available-slots")
                        .queryParam("providerId", "prov-44")
                        .queryParam("date", "2026-06-15")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.providerId").isEqualTo("prov-44")
                .jsonPath("$.data.date").isEqualTo("2026-06-15");

        assertThat(lastMethod.get()).isEqualTo("GET");
        assertThat(lastPath.get()).isEqualTo("/appointments/available-slots");
        assertThat(lastQuery.get()).isNotBlank();
        assertThat(lastQuery.get()).contains("providerId=prov-44");
        assertThat(lastQuery.get()).contains("date=2026-06-15");
    }

    @Test
    void shouldForwardTeleconsultStartToDownstreamWithStrippedPrefix() {
        webTestClient.post()
                .uri("/api/appointments/apt-2001/teleconsult/start")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.status").isEqualTo("INITIATED")
                .jsonPath("$.data.sessionId").isEqualTo("tcs-5001");

        assertThat(lastMethod.get()).isEqualTo("POST");
        assertThat(lastPath.get()).isEqualTo("/appointments/apt-2001/teleconsult/start");
        assertThat(lastBody.get()).isEmpty();
    }

    @Test
    void shouldForwardTeleconsultCompleteBodyToDownstreamWithStrippedPrefix() {
        String payload = """
                {
                  "consultationNotes": "Consultation completed and medication plan reviewed."
                }
                """;

        webTestClient.post()
                .uri("/api/appointments/apt-2001/teleconsult/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.status").isEqualTo("COMPLETED")
                .jsonPath("$.data.consultationNotes").isEqualTo("Consultation completed and medication plan reviewed.");

        assertThat(lastMethod.get()).isEqualTo("POST");
        assertThat(lastPath.get()).isEqualTo("/appointments/apt-2001/teleconsult/complete");
        assertThat(lastBody.get()).contains("consultationNotes");
        assertThat(lastBody.get()).contains("Consultation completed and medication plan reviewed.");
    }

    @Test
    void shouldPassThroughTeleconsultDownstreamServiceUnavailable() {
        String payload = """
                {
                  "consultationNotes": "Retry due to temporary downstream outage."
                }
                """;

        webTestClient.post()
                .uri("/api/appointments/apt-5001/teleconsult/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.code").isEqualTo("TELECONSULTATION_NETWORK_FAILURE")
                .jsonPath("$.message").isEqualTo("Medical-record service unavailable");

        assertThat(lastMethod.get()).isEqualTo("POST");
        assertThat(lastPath.get()).isEqualTo("/appointments/apt-5001/teleconsult/complete");
        assertThat(lastBody.get()).contains("Retry due to temporary downstream outage.");
    }

    @Test
    void shouldPassThroughTeleconsultJoinDownstreamNotFound() {
        webTestClient.post()
                .uri("/api/appointments/apt-4041/teleconsult/join")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("TELECONSULTATION_NOT_FOUND")
                .jsonPath("$.message").isEqualTo("Teleconsultation session not found for appointmentId=apt-4041");

        assertThat(lastMethod.get()).isEqualTo("POST");
        assertThat(lastPath.get()).isEqualTo("/appointments/apt-4041/teleconsult/join");
        assertThat(lastBody.get()).isEmpty();
    }

    @Test
    void shouldPassThroughTeleconsultCompleteDownstreamConflict() {
        String payload = """
                {
                  "consultationNotes": "Duplicate completion attempt."
                }
                """;

        webTestClient.post()
                .uri("/api/appointments/apt-4091/teleconsult/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("APPOINTMENT_NOT_ELIGIBLE")
                .jsonPath("$.message").isEqualTo("appointmentId=apt-4091 not eligible for teleconsultation; teleconsultation already completed");

        assertThat(lastMethod.get()).isEqualTo("POST");
        assertThat(lastPath.get()).isEqualTo("/appointments/apt-4091/teleconsult/complete");
        assertThat(lastBody.get()).contains("Duplicate completion attempt.");
    }

    private static String readBody(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        if (body.length == 0) {
            return "";
        }
        return new String(body, StandardCharsets.UTF_8);
    }
}
