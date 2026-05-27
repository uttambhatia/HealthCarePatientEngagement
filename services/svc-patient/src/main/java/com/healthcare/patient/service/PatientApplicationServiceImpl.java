package com.healthcare.patient.service;

        import com.healthcare.patient.domain.PatientProfile;
        import com.healthcare.patient.dto.CreatePatientRequest;
        import com.healthcare.patient.dto.PatientResponse;
        import com.healthcare.patient.event.PatientRegisteredEvent;
        import com.healthcare.patient.exception.ResourceNotFoundException;
        import com.healthcare.patient.integration.PatientFhirAdapter;
        import com.healthcare.patient.repository.PatientRepository;
        import com.healthcare.platform.common.messaging.MessagingPort;
        import org.springframework.stereotype.Service;

        import java.util.List;
        import java.util.UUID;

        @Service
        public class PatientApplicationServiceImpl implements PatientApplicationService {
            private final PatientRepository repository;
            private final MessagingPort messagingPort;
            private final PatientFhirAdapter integration;

            public PatientApplicationServiceImpl(PatientRepository repository, MessagingPort messagingPort, PatientFhirAdapter integration) {
                this.repository = repository;
                this.messagingPort = messagingPort;
                this.integration = integration;
            }

            @Override
            public PatientResponse registerPatient(CreatePatientRequest request, String correlationId) {
                PatientProfile aggregate = repository.save(new PatientProfile(
                        UUID.randomUUID().toString(),
                        "ACTIVE",
                        request.externalReference(),
                request.givenName(),
                request.familyName(),
                request.birthDate()
                ));
                integration.synchronizeProfile(aggregate, correlationId);
                messagingPort.publish("patient-service", correlationId, new PatientRegisteredEvent(
                        aggregate.id(),
                        aggregate.externalReference(),
                        aggregate.givenName(),
                        aggregate.familyName(),
                        aggregate.birthDate()
                ));
                return map(aggregate);
            }

            @Override
            public PatientResponse getPatient(String id) {
                return repository.findById(id).map(this::map)
                        .orElseThrow(() -> new ResourceNotFoundException("Patient record not found: " + id));
            }

            @Override
            public List<PatientResponse> listPatients() {
                return repository.findAll().stream().map(this::map).toList();
            }


public PatientResponse orchestrateOnboarding(CreatePatientRequest request, String correlationId) {
    PatientResponse response = registerPatient(request, correlationId);
    try {
        return response;
    } catch (RuntimeException exception) {
        repository.deleteById(response.id());
        throw exception;
    }
}
            private PatientResponse map(PatientProfile aggregate) {
                return new PatientResponse(
                        aggregate.id(),
                        aggregate.status(),
                        aggregate.externalReference(),
                aggregate.givenName(),
                aggregate.familyName(),
                aggregate.birthDate()
                );
            }
        }
