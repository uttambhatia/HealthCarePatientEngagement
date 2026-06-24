package com.healthcare.patient.integration;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.healthcare.patient.domain.PatientProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class PatientFhirAdapterContractTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
    }

    @Test
    void shouldRetryAndFailWhenFhirSyncKeepsFailing() {
        TokenCredential credential = new TokenCredential() {
            @Override
            public reactor.core.publisher.Mono<AccessToken> getToken(TokenRequestContext request) {
                return reactor.core.publisher.Mono.just(new AccessToken("test-token", OffsetDateTime.now().plusHours(1)));
            }
        };

        PatientFhirAdapter adapter = new PatientFhirAdapter(
                restClientBuilder,
                "http://fhir.example.internal",
                "https://fhir.example.internal",
                2,
                credential
        );

        PatientProfile profile = new PatientProfile(
                "pat-1",
                "ACTIVE",
                null,
                "EXT-1",
                "Ava",
                "Jones",
                "1985-04-12",
                "ava.jones@example.com",
                "+1-555-1000",
                "FEMALE",
                null,
                null
        );

        server.expect(ExpectedCount.times(2), requestTo("http://fhir.example.internal/Patient/pat-1"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header("Authorization", "Bearer " + "test-token"))
                .andExpect(header("X-Correlation-Id", "corr-123"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> adapter.synchronizeProfile(profile, "corr-123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("failed after retries");

        server.verify();
    }
}
