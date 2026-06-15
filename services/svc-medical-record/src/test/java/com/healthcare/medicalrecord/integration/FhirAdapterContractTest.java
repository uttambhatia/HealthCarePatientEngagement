package com.healthcare.medicalrecord.integration;

import com.healthcare.medicalrecord.domain.MedicalRecordSnapshot;
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

class FhirAdapterContractTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
    }

    @Test
    void shouldRetryAndFailWhenFhirUpsertKeepsFailing() {
        FhirAdapter adapter = new FhirAdapter(
                restClientBuilder,
                "http://fhir.example.internal",
                "/fhir/medical-records",
                2
        );

        MedicalRecordSnapshot snapshot = new MedicalRecordSnapshot(
                "mr-1",
                "SYNCED",
                "pat-1",
                "Observation",
                "Observation/obs-1",
                "HbA1c measured at 7.4",
                1
        );

        server.expect(ExpectedCount.times(2), requestTo("http://fhir.example.internal/fhir/medical-records"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Correlation-Id", "corr-123"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> adapter.upsertFhirResource(snapshot, "corr-123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("failed after retries");

        server.verify();
    }
}
