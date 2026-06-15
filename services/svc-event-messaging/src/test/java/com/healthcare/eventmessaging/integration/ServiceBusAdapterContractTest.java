package com.healthcare.eventmessaging.integration;

import com.healthcare.eventmessaging.domain.ServiceBusMessageRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class ServiceBusAdapterContractTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
    }

    @Test
    void shouldRetryAndFailWhenQueueOnBusKeepsFailing() {
        ServiceBusAdapter adapter = new ServiceBusAdapter(
                restClientBuilder,
                "http://service-bus-adapter.internal",
                "/servicebus/messages",
                2
        );

        ServiceBusMessageRecord record = new ServiceBusMessageRecord(
                "msg-1",
                "QUEUED",
                "care-events",
                "AppointmentBooked",
                "{\"appointmentId\":\"apt-2001\"}",
                "DOMAIN_EVENT",
                "2026-06-06T10:15:00Z",
                "hash-1",
                null
        );

        server.expect(ExpectedCount.times(2), requestTo("http://service-bus-adapter.internal/servicebus/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Correlation-Id", "corr-123"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> adapter.queueOnBus(record, "corr-123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("failed after retries");

        server.verify();
    }
}
