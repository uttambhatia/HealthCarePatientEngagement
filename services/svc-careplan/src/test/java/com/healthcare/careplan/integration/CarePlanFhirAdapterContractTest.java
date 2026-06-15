package com.healthcare.careplan.integration;

import com.healthcare.careplan.domain.CarePlanAggregate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class CarePlanFhirAdapterContractTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
    }

    @Test
    void shouldRetryAndFailWhenCarePlanSyncKeepsFailing() {
        CarePlanFhirAdapter adapter = new CarePlanFhirAdapter(
                restClientBuilder,
                "http://fhir.example.internal",
                "/fhir/careplans",
                2
        );

        CarePlanAggregate aggregate = new CarePlanAggregate(
                "cp-1",
                "MANAGED",
                "pat-1",
                "Improve glycemic control",
                "ACTIVE",
                "clinician-1",
                List.of("Track glucose"),
                1
        );

        server.expect(ExpectedCount.times(2), requestTo("http://fhir.example.internal/fhir/careplans"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Correlation-Id", "corr-123"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> adapter.synchronizeCarePlan(aggregate, "corr-123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("failed after retries");

        server.verify();
    }
}
