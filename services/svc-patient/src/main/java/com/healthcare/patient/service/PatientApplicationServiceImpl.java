package com.healthcare.patient.service;

        import com.healthcare.patient.domain.PatientProfile;
        import com.healthcare.patient.dto.CreatePatientRequest;
        import com.healthcare.patient.dto.PatientResponse;
        import com.healthcare.patient.event.PatientRegisteredEvent;
    import com.healthcare.patient.exception.DuplicateRegistrationException;
        import com.healthcare.patient.exception.ResourceNotFoundException;
    import com.healthcare.patient.integration.PatientIdentityAdapter;
        import com.healthcare.patient.integration.PatientFhirAdapter;
    import com.healthcare.patient.integration.PatientNotificationAdapter;
        import com.healthcare.patient.repository.PatientRepository;
        import com.healthcare.platform.common.messaging.MessagingPort;
        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;
        import org.springframework.stereotype.Service;

        import java.util.List;
        import java.util.UUID;

        @Service
        public class PatientApplicationServiceImpl implements PatientApplicationService {
            private static final Logger LOGGER = LoggerFactory.getLogger(PatientApplicationServiceImpl.class);

            private final PatientRepository repository;
            private final MessagingPort messagingPort;
            private final PatientFhirAdapter integration;
                    private final PatientIdentityAdapter identityAdapter;
                    private final PatientNotificationAdapter notificationAdapter;

                    public PatientApplicationServiceImpl(
                            PatientRepository repository,
                            MessagingPort messagingPort,
                            PatientFhirAdapter integration,
                            PatientIdentityAdapter identityAdapter,
                            PatientNotificationAdapter notificationAdapter) {
                this.repository = repository;
                this.messagingPort = messagingPort;
                this.integration = integration;
                        this.identityAdapter = identityAdapter;
                        this.notificationAdapter = notificationAdapter;
            }

            @Override
            public PatientResponse registerPatient(CreatePatientRequest request, String correlationId) {
                        validateDuplicateRegistration(request);
                        identityAdapter.provisionIdentity(request, correlationId);

                PatientProfile aggregate = repository.save(new PatientProfile(
                        UUID.randomUUID().toString(),
                        "ACTIVE",
                        request.externalReference(),
                request.givenName(),
                request.familyName(),
                        request.birthDate(),
                        request.email(),
                        request.phone(),
                        request.demographics()
                ));
                    synchronizeProfileSafely(aggregate, correlationId);
                    publishRegistrationEventSafely(aggregate, correlationId);
                    sendRegistrationConfirmationSafely(aggregate, correlationId);
                    return map(aggregate);
            }

                    private void validateDuplicateRegistration(CreatePatientRequest request) {
                        if (repository.existsByExternalReference(request.externalReference())) {
                            throw new DuplicateRegistrationException("Patient already registered with externalReference=" + request.externalReference());
                        }
                        if (repository.existsByEmail(request.email())) {
                            throw new DuplicateRegistrationException("Patient already registered with email=" + request.email());
                        }
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
    return registerPatient(request, correlationId);
}

            private void synchronizeProfileSafely(PatientProfile aggregate, String correlationId) {
                try {
                    integration.synchronizeProfile(aggregate, correlationId);
                } catch (RuntimeException exception) {
                    LOGGER.warn("Patient FHIR synchronization failed aggregateId={} correlationId={} error={}",
                            aggregate.id(), correlationId, exception.getMessage());
                }
            }

            private void publishRegistrationEventSafely(PatientProfile aggregate, String correlationId) {
                try {
                    messagingPort.publish("patient-service", correlationId, new PatientRegisteredEvent(
                            aggregate.id(),
                            aggregate.externalReference(),
                            aggregate.givenName(),
                            aggregate.familyName(),
                            aggregate.birthDate(),
                            aggregate.email(),
                            aggregate.phone(),
                            aggregate.demographics()
                    ));
                } catch (RuntimeException exception) {
                    LOGGER.warn("Patient registration event publish failed aggregateId={} correlationId={} error={}",
                            aggregate.id(), correlationId, exception.getMessage());
                }
            }

            private void sendRegistrationConfirmationSafely(PatientProfile aggregate, String correlationId) {
                try {
                    notificationAdapter.sendRegistrationConfirmation(aggregate, correlationId);
                } catch (RuntimeException exception) {
                    LOGGER.warn("Patient registration notification failed aggregateId={} correlationId={} error={}",
                            aggregate.id(), correlationId, exception.getMessage());
                }
            }

            private PatientResponse map(PatientProfile aggregate) {
                return new PatientResponse(
                        aggregate.id(),
                        aggregate.status(),
                        aggregate.externalReference(),
                aggregate.givenName(),
                aggregate.familyName(),
                aggregate.birthDate(),
                aggregate.email(),
                aggregate.phone(),
                aggregate.demographics()
                );
            }
        }
