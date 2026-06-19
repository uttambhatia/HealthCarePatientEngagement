package com.healthcare.patient.service;

import com.healthcare.patient.domain.PatientProfile;
import com.healthcare.patient.dto.CreatePatientRequest;
import com.healthcare.patient.event.PatientRegisteredEvent;
import com.healthcare.patient.event.PatientOnboardingRequestedEvent;
import com.healthcare.patient.integration.PatientFhirAdapter;
import com.healthcare.patient.integration.PatientDocumentStorageAdapter;
import com.healthcare.patient.integration.PatientIdentityAdapter;
import com.healthcare.patient.integration.PatientNotificationAdapter;
import com.healthcare.patient.repository.PatientRepository;
import com.healthcare.platform.common.event.DomainEvent;
import com.healthcare.platform.common.messaging.MessagingPort;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class PatientApplicationServiceImplTest {

    @Test
    void shouldRegisterAndFetchPatient() {
        InMemoryPatientRepository repository = new InMemoryPatientRepository();
        RecordingMessagingPort messagingPort = new RecordingMessagingPort();
        PatientApplicationService service = new PatientApplicationServiceImpl(
                repository,
                messagingPort,
                new PatientFhirAdapter(RestClient.builder(), "", "/fhir/patients", 1),
                new PatientIdentityAdapter(RestClient.builder(), "", "/identity/assertions"),
                new PatientNotificationAdapter(RestClient.builder(), "", "/notifications"),
                new PatientDocumentStorageAdapter("", "patient-id-proofs")
        );

        var created = service.registerPatient(new CreatePatientRequest(
                "EXT-100",
                "Ava",
                "Jones",
                "1985-04-12",
                "ava.jones@example.com",
                "+1-555-1000",
                "FEMALE"
        ), "corr-123");

        assertThat(created.id()).isNotBlank();
        assertThat(service.getPatient(created.id()).externalReference()).isEqualTo("EXT-100");
        assertThat(service.getPatient(created.id()).email()).isEqualTo("ava.jones@example.com");
                assertThat(messagingPort.events).hasSize(1);
    }

    @Test
    void shouldKeepRegistrationWhenFhirSyncFails() {
        InMemoryPatientRepository repository = new InMemoryPatientRepository();
        RecordingMessagingPort messagingPort = new RecordingMessagingPort();
        PatientApplicationService localService = new PatientApplicationServiceImpl(
                repository,
                messagingPort,
                new PatientFhirAdapter(RestClient.builder(), "", "/fhir/patients", 1),
                new PatientIdentityAdapter(RestClient.builder(), "", "/identity/assertions"),
                new PatientNotificationAdapter(RestClient.builder(), "", "/notifications"),
                new PatientDocumentStorageAdapter("", "patient-id-proofs")
        );

        var created = localService.registerPatient(new CreatePatientRequest(
                "EXT-200",
                "Ava",
                "Retry",
                "1985-04-12",
                "ava.retry@example.com",
                "+1-555-2000",
                "FEMALE"
        ), "corr-456");

        assertThat(created.id()).isNotBlank();
        assertThat(created.externalReference()).isEqualTo("EXT-200");
                assertThat(repository.findById(created.id())).hasValueSatisfying(saved ->
                                assertThat(saved.externalReference()).isEqualTo("EXT-200"));
                assertThat(messagingPort.events).hasSize(1);
                assertThat(messagingPort.events.get(0)).isInstanceOf(PatientRegisteredEvent.class);
    }

    @Test
    void shouldPublishOnboardingEventWhenPatientIsApproved() {
        InMemoryPatientRepository repository = new InMemoryPatientRepository();
        RecordingMessagingPort messagingPort = new RecordingMessagingPort();
        PatientApplicationService service = new PatientApplicationServiceImpl(
                repository,
                messagingPort,
                new PatientFhirAdapter(RestClient.builder(), "", "/fhir/patients", 1),
                new PatientIdentityAdapter(RestClient.builder(), "", "/identity/assertions"),
                new PatientNotificationAdapter(RestClient.builder(), "", "/notifications"),
                new PatientDocumentStorageAdapter("", "patient-id-proofs")
        );

        var created = service.registerPatient(new CreatePatientRequest(
                "EXT-300",
                "Ava",
                "Approved",
                "1985-04-12",
                "ava.approved@example.com",
                "+1-555-3000",
                "FEMALE"
        ), "corr-789");

        var approved = service.approveRegistration(created.id(), "admin@example.com", "corr-790");

        assertThat(approved.status()).isEqualToIgnoringCase("COMPLETED");
        assertThat(messagingPort.events).hasSize(2);
        assertThat(messagingPort.events.get(1)).isInstanceOf(PatientOnboardingRequestedEvent.class);
        assertThat(((PatientOnboardingRequestedEvent) messagingPort.events.get(1)).targetRole()).isEqualTo("PATIENT");
    }

        private static final class InMemoryPatientRepository implements PatientRepository {
                private final Map<String, PatientProfile> byId = new LinkedHashMap<>();

                @Override
                public PatientProfile save(PatientProfile aggregate) {
                        byId.put(aggregate.id(), aggregate);
                        return aggregate;
                }

                @Override
                public Optional<PatientProfile> findById(String id) {
                        return Optional.ofNullable(byId.get(id));
                }

                @Override
                public List<PatientProfile> findAll() {
                        return new ArrayList<>(byId.values());
                }

                @Override
                public boolean existsByExternalReference(String externalReference) {
                        return byId.values().stream().anyMatch(patient -> patient.externalReference().equalsIgnoreCase(externalReference));
                }

                @Override
                public boolean existsByEmail(String email) {
                        return byId.values().stream().anyMatch(patient -> patient.email().equalsIgnoreCase(email));
                }

                @Override
                public void deleteById(String id) {
                        byId.remove(id);
                }
        }

        private static final class RecordingMessagingPort implements MessagingPort {
                private final List<DomainEvent> events = new ArrayList<>();

                @Override
                public void publish(String channel, String correlationId, DomainEvent event) {
                        events.add(event);
                }
        }
}
