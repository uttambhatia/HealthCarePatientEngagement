package com.healthcare.patient.service;

        import com.healthcare.patient.domain.PatientProfile;
        import com.healthcare.patient.dto.CreatePatientRequest;
        import com.healthcare.patient.dto.PatientDocumentDownload;
        import com.healthcare.patient.dto.PatientResponse;
        import com.healthcare.patient.event.PatientRegisteredEvent;
        import com.healthcare.patient.event.PatientOnboardingRequestedEvent;
    import com.healthcare.patient.exception.DuplicateRegistrationException;
        import com.healthcare.patient.exception.ResourceNotFoundException;
    import com.healthcare.patient.integration.PatientIdentityAdapter;
        import com.healthcare.patient.integration.PatientDocumentStorageAdapter;
        import com.healthcare.patient.integration.PatientFhirAdapter;
    import com.healthcare.patient.integration.PatientNotificationAdapter;
        import com.healthcare.patient.repository.PatientRepository;
        import com.healthcare.platform.common.messaging.MessagingPort;
        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;
        import org.springframework.stereotype.Service;
        import org.springframework.web.multipart.MultipartFile;

        import java.time.Instant;
        import java.util.List;
        import java.util.UUID;

        @Service
        public class PatientApplicationServiceImpl implements PatientApplicationService {
            private static final Logger LOGGER = LoggerFactory.getLogger(PatientApplicationServiceImpl.class);
            private static final String STATUS_PENDING_VERIFICATION = "PENDING_VERIFICATION";
            private static final String STATUS_COMPLETED = "COMPLETED";
            private static final String STATUS_REJECTED = "REJECTED";

            private final PatientRepository repository;
            private final MessagingPort messagingPort;
            private final PatientFhirAdapter integration;
                    private final PatientIdentityAdapter identityAdapter;
                    private final PatientNotificationAdapter notificationAdapter;
                    private final PatientDocumentStorageAdapter documentStorageAdapter;

                    public PatientApplicationServiceImpl(
                            PatientRepository repository,
                            MessagingPort messagingPort,
                            PatientFhirAdapter integration,
                            PatientIdentityAdapter identityAdapter,
                                PatientNotificationAdapter notificationAdapter,
                                PatientDocumentStorageAdapter documentStorageAdapter) {
                this.repository = repository;
                this.messagingPort = messagingPort;
                this.integration = integration;
                        this.identityAdapter = identityAdapter;
                        this.notificationAdapter = notificationAdapter;
                        this.documentStorageAdapter = documentStorageAdapter;
            }

            @Override
            public PatientResponse registerPatient(CreatePatientRequest request, String correlationId) {
                        validateDuplicateRegistration(request);

                PatientProfile aggregate = repository.save(new PatientProfile(
                        UUID.randomUUID().toString(),
                    STATUS_PENDING_VERIFICATION,
                    null,
                        request.externalReference(),
                request.givenName(),
                request.familyName(),
                        request.birthDate(),
                        request.email(),
                        request.phone(),
                        request.demographics(),
                        null,
                        null
                ));
                        sendPendingVerificationNotificationSafely(aggregate, correlationId);
                    publishRegistrationEventSafely(aggregate, correlationId);
                    return map(aggregate);
            }

                    @Override
                    public PatientResponse approveRegistration(String id, String actor, String correlationId) {
                    PatientProfile current = repository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Patient record not found: " + id));

                    if (STATUS_COMPLETED.equalsIgnoreCase(current.status())) {
                        return map(current);
                    }

                    PatientProfile completed = new PatientProfile(
                        current.id(),
                        STATUS_COMPLETED,
                        buildDecisionAudit("APPROVED", actor, correlationId),
                        current.externalReference(),
                        current.givenName(),
                        current.familyName(),
                        current.birthDate(),
                        current.email(),
                        current.phone(),
                        current.demographics(),
                        current.idProofBlobName(),
                        current.idProofFileName()
                    );

                    synchronizeProfileSafely(completed, correlationId);
                    identityAdapter.provisionIdentity(completed, correlationId);
                    PatientProfile saved = repository.save(completed);
                    publishApprovalEventSafely(saved, correlationId);
                    sendApprovalNotificationSafely(saved, correlationId);
                    return map(saved);
                    }

                    @Override
                    public PatientResponse rejectRegistration(String id, String actor, String correlationId) {
                    PatientProfile current = repository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Patient record not found: " + id));

                    if (STATUS_REJECTED.equalsIgnoreCase(current.status())) {
                        return map(current);
                    }

                    PatientProfile rejected = new PatientProfile(
                        current.id(),
                        STATUS_REJECTED,
                        buildDecisionAudit("REJECTED", actor, correlationId),
                        current.externalReference(),
                        current.givenName(),
                        current.familyName(),
                        current.birthDate(),
                        current.email(),
                        current.phone(),
                        current.demographics(),
                        current.idProofBlobName(),
                        current.idProofFileName()
                    );

                    PatientProfile saved = repository.save(rejected);
                    synchronizeProfileSafely(saved, correlationId);
                    sendRejectionNotificationSafely(saved, correlationId);
                    return map(saved);
                    }

                    @Override
                    public PatientResponse resendRegistrationNotification(String id, String correlationId) {
                    PatientProfile aggregate = repository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Patient record not found: " + id));

                    resendRegistrationNotificationSafely(aggregate, correlationId);
                    return map(aggregate);
                    }

                    @Override
                    public PatientResponse uploadIdProof(String id, MultipartFile file, String correlationId) {
                    PatientProfile current = repository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Patient record not found: " + id));

                    PatientDocumentStorageAdapter.StoredDocument storedDocument =
                        documentStorageAdapter.uploadIdProof(id, file, correlationId);

                    PatientProfile updated = new PatientProfile(
                        current.id(),
                        current.status(),
                        current.decisionAudit(),
                        current.externalReference(),
                        current.givenName(),
                        current.familyName(),
                        current.birthDate(),
                        current.email(),
                        current.phone(),
                        current.demographics(),
                        storedDocument.blobName(),
                        storedDocument.originalFileName()
                    );
                    return map(repository.save(updated));
                    }

                    @Override
                    public PatientDocumentDownload downloadIdProof(String id, String correlationId) {
                    PatientProfile current = repository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Patient record not found: " + id));
                    if (current.idProofBlobName() == null || current.idProofBlobName().isBlank()) {
                        throw new ResourceNotFoundException("ID proof not uploaded for patient: " + id);
                    }

                    PatientDocumentStorageAdapter.DownloadedDocument downloaded =
                        documentStorageAdapter.download(current.idProofBlobName(), correlationId);
                    return new PatientDocumentDownload(downloaded.content(), downloaded.contentType(), downloaded.fileName());
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

            private void publishApprovalEventSafely(PatientProfile aggregate, String correlationId) {
                try {
                    messagingPort.publish("identity-adapter-service", correlationId, new PatientOnboardingRequestedEvent(
                            aggregate.id(),
                            aggregate.externalReference(),
                            aggregate.givenName(),
                            aggregate.familyName(),
                            aggregate.email(),
                            "PATIENT"
                    ));
                } catch (RuntimeException exception) {
                    LOGGER.warn("Patient approval event publish failed aggregateId={} correlationId={} error={}",
                            aggregate.id(), correlationId, exception.getMessage());
                }
            }

            private void sendPendingVerificationNotificationSafely(PatientProfile aggregate, String correlationId) {
                try {
                    notificationAdapter.sendPendingVerification(aggregate, correlationId);
                } catch (RuntimeException exception) {
                    LOGGER.warn("Patient pending-verification notification failed aggregateId={} correlationId={} error={}",
                            aggregate.id(), correlationId, exception.getMessage());
                }
            }

            private void sendApprovalNotificationSafely(PatientProfile aggregate, String correlationId) {
                try {
                    notificationAdapter.sendApprovalNotification(aggregate, correlationId);
                } catch (RuntimeException exception) {
                    LOGGER.warn("Patient approval notification failed aggregateId={} correlationId={} error={}",
                            aggregate.id(), correlationId, exception.getMessage());
                }
            }

            private void sendRejectionNotificationSafely(PatientProfile aggregate, String correlationId) {
                try {
                    notificationAdapter.sendRejectionNotification(aggregate, correlationId);
                } catch (RuntimeException exception) {
                    LOGGER.warn("Patient rejection notification failed aggregateId={} correlationId={} error={}",
                            aggregate.id(), correlationId, exception.getMessage());
                }
            }

            private void resendRegistrationNotificationSafely(PatientProfile aggregate, String correlationId) {
                try {
                    notificationAdapter.resendNotification(aggregate, correlationId);
                } catch (RuntimeException exception) {
                    LOGGER.warn("Patient notification resend failed aggregateId={} correlationId={} error={}",
                            aggregate.id(), correlationId, exception.getMessage());
                }
            }

            private PatientResponse map(PatientProfile aggregate) {
                return new PatientResponse(
                        aggregate.id(),
                        aggregate.status(),
                        aggregate.decisionAudit(),
                        aggregate.externalReference(),
                aggregate.givenName(),
                aggregate.familyName(),
                aggregate.birthDate(),
                aggregate.email(),
                aggregate.phone(),
                aggregate.demographics(),
                aggregate.idProofBlobName() != null && !aggregate.idProofBlobName().isBlank(),
                aggregate.idProofFileName()
                );
            }

            private String buildDecisionAudit(String action, String actor, String correlationId) {
                String normalizedActor = (actor == null || actor.isBlank()) ? "unknown" : actor;
                return "%s|%s|%s|%s".formatted(action, Instant.now(), normalizedActor, correlationId);
            }
        }
