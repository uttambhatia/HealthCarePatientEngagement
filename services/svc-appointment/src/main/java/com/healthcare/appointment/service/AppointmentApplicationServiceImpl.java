package com.healthcare.appointment.service;

import com.healthcare.appointment.domain.AppointmentRecord;
import com.healthcare.appointment.domain.TeleconsultationSession;
import com.healthcare.appointment.dto.AppointmentResponse;
import com.healthcare.appointment.dto.AvailableSlotResponse;
import com.healthcare.appointment.dto.CompleteTeleconsultationRequest;
import com.healthcare.appointment.dto.CreateAppointmentRequest;
import com.healthcare.appointment.dto.TeleconsultationResponse;
import com.healthcare.appointment.event.AppointmentBookedEvent;
import com.healthcare.appointment.event.TeleconsultationCompletedEvent;
import com.healthcare.appointment.event.TeleconsultationStartedEvent;
import com.healthcare.appointment.exception.AppointmentNotEligibleException;
import com.healthcare.appointment.exception.InsecureSessionConfigurationException;
import com.healthcare.appointment.exception.ResourceNotFoundException;
import com.healthcare.appointment.exception.SlotAlreadyBookedException;
import com.healthcare.appointment.exception.TeleconsultationSessionNotFoundException;
import com.healthcare.appointment.integration.AppointmentNotificationAdapter;
import com.healthcare.appointment.integration.ConsentAccessAdapter;
import com.healthcare.appointment.integration.TeleconsultationAcsAdapter;
import com.healthcare.appointment.integration.TeleconsultationMedicalRecordAdapter;
import com.healthcare.appointment.repository.AppointmentRepository;
import com.healthcare.appointment.repository.TeleconsultationSessionRepository;
import com.healthcare.platform.common.audit.AuditLogger;
import com.healthcare.platform.common.messaging.MessagingPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class AppointmentApplicationServiceImpl implements AppointmentApplicationService {
    private static final String BOOKED = "BOOKED";
    private static final String TELECONSULT_INITIATED = "INITIATED";
    private static final String TELECONSULT_IN_PROGRESS = "IN_PROGRESS";
    private static final String TELECONSULT_COMPLETED = "COMPLETED";
    private static final LocalTime SLOT_START = LocalTime.of(9, 0);
    private static final LocalTime SLOT_END_EXCLUSIVE = LocalTime.of(17, 0);

    private final AppointmentRepository repository;
    private final MessagingPort messagingPort;
    private final AppointmentNotificationAdapter integration;
    private final ConsentAccessAdapter consentAccessAdapter;
    private final TeleconsultationSessionRepository teleconsultationRepository;
    private final TeleconsultationMedicalRecordAdapter teleconsultationMedicalRecordAdapter;
    private final TeleconsultationAcsAdapter teleconsultationAcsAdapter;
    private final AuditLogger auditLogger;
    private final String secureTeleconsultBaseUrl;

    @Value("${platform.messaging.teleconsultation.completedChannel:topic:teleconsultation-completed}")
    private String teleconsultationCompletedChannel;

    public AppointmentApplicationServiceImpl(
            AppointmentRepository repository,
            MessagingPort messagingPort,
            AppointmentNotificationAdapter integration,
            ConsentAccessAdapter consentAccessAdapter,
            TeleconsultationSessionRepository teleconsultationRepository,
            TeleconsultationMedicalRecordAdapter teleconsultationMedicalRecordAdapter,
            TeleconsultationAcsAdapter teleconsultationAcsAdapter,
            AuditLogger auditLogger,
            @Value("${platform.integration.teleconsult.secure-base-url:https://teleconsult.healthcare.local}") String secureTeleconsultBaseUrl) {
        this.repository = repository;
        this.messagingPort = messagingPort;
        this.integration = integration;
        this.consentAccessAdapter = consentAccessAdapter;
        this.teleconsultationRepository = teleconsultationRepository;
        this.teleconsultationMedicalRecordAdapter = teleconsultationMedicalRecordAdapter;
        this.teleconsultationAcsAdapter = teleconsultationAcsAdapter;
        this.auditLogger = auditLogger;
        this.secureTeleconsultBaseUrl = secureTeleconsultBaseUrl;
    }

    @Override
    public AppointmentResponse bookAppointment(CreateAppointmentRequest request, String correlationId) {
        consentAccessAdapter.ensureAccessAllowed(request.patientId(), correlationId);

        if (repository.existsBookedSlot(request.providerId(), request.scheduledAt())) {
            throw new SlotAlreadyBookedException(request.providerId(), request.scheduledAt());
        }

        AppointmentRecord aggregate;
        try {
            aggregate = repository.save(new AppointmentRecord(
                    UUID.randomUUID().toString(),
                    BOOKED,
                    request.patientId(),
                    request.providerId(),
                    request.scheduledAt(),
                    request.channel()
            ));
        } catch (DataIntegrityViolationException conflict) {
            throw new SlotAlreadyBookedException(request.providerId(), request.scheduledAt());
        }

        try {
            integration.sendBookingNotification(aggregate, correlationId);
            messagingPort.publish("appointment-service", correlationId, new AppointmentBookedEvent(
                    aggregate.id(),
                    aggregate.patientId(),
                    aggregate.providerId(),
                    aggregate.scheduledAt(),
                    aggregate.channel()
            ));
            return map(aggregate);
        } catch (RuntimeException exception) {
            repository.deleteById(aggregate.id());
            throw exception;
        }
    }

    @Override
    public AppointmentResponse getAppointment(String id) {
        return repository.findById(id).map(this::map)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment record not found: " + id));
    }

    @Override
    public List<AppointmentResponse> listAppointments() {
        return repository.findAll().stream().map(this::map).toList();
    }

    @Override
    public AvailableSlotResponse listAvailableSlots(String providerId, String date) {
        String normalizedDate = LocalDate.parse(date).toString();

        Set<String> bookedSlots = repository.findBookedSlotsByProviderAndDate(providerId, normalizedDate)
                .stream()
                .map(AppointmentRecord::scheduledAt)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        List<String> availableSlots = buildDaySlots(normalizedDate).stream()
                .filter(slot -> !bookedSlots.contains(slot))
                .toList();

        return new AvailableSlotResponse(providerId, normalizedDate, availableSlots);
    }

    @Override
    public TeleconsultationResponse startTeleconsultation(String appointmentId, String correlationId) {
        AppointmentRecord appointment = getBookedAppointment(appointmentId);
        validateSecureTeleconsultBaseUrl();

        TeleconsultationSession existing = teleconsultationRepository.findByAppointmentId(appointmentId).orElse(null);
        if (existing != null) {
            if (TELECONSULT_COMPLETED.equals(existing.status())) {
                throw new AppointmentNotEligibleException(appointmentId, "teleconsultation already completed");
            }
            return mapTeleconsultation(existing);
        }

        String sessionId = UUID.randomUUID().toString();
        String startedAt = now();
        TeleconsultationAcsAdapter.TeleconsultJoinUrls joinUrls = teleconsultationAcsAdapter
            .createSession(appointment.id(), appointment.patientId(), appointment.providerId(), correlationId)
            .orElseGet(() -> new TeleconsultationAcsAdapter.TeleconsultJoinUrls(
                secureTeleconsultBaseUrl + "/session/" + sessionId + "?role=DOCTOR",
                secureTeleconsultBaseUrl + "/session/" + sessionId + "?role=PATIENT"));

        TeleconsultationSession session = teleconsultationRepository.save(new TeleconsultationSession(
                sessionId,
                appointment.id(),
                appointment.patientId(),
                appointment.providerId(),
                TELECONSULT_INITIATED,
                joinUrls.doctorJoinUrl(),
                joinUrls.patientJoinUrl(),
                startedAt,
                null,
                null,
                null,
                false,
                null,
                List.of(),
                List.of(logEntry("Teleconsultation initiated by doctor"))
        ));

        messagingPort.publish("appointment-service", correlationId, new TeleconsultationStartedEvent(
                session.id(),
                session.appointmentId(),
                session.patientId(),
                session.providerId(),
                session.startedAt()
        ));

        auditLogger.log(resolveActor(), "START_TELECONSULTATION", appointmentId, correlationId);
        return mapTeleconsultation(session);
    }

    @Override
    public TeleconsultationResponse joinTeleconsultation(String appointmentId, String correlationId) {
        TeleconsultationSession session = teleconsultationRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new TeleconsultationSessionNotFoundException(appointmentId));

        enforcePatientScope(session.patientId());

        if (TELECONSULT_COMPLETED.equals(session.status())) {
            throw new AppointmentNotEligibleException(appointmentId, "teleconsultation already completed");
        }

        if (TELECONSULT_IN_PROGRESS.equals(session.status())) {
            return mapTeleconsultation(session);
        }

        TeleconsultationSession updated = teleconsultationRepository.save(new TeleconsultationSession(
                session.id(),
                session.appointmentId(),
                session.patientId(),
                session.providerId(),
                TELECONSULT_IN_PROGRESS,
                session.doctorJoinUrl(),
                session.patientJoinUrl(),
                session.startedAt(),
                now(),
                session.completedAt(),
                session.consultationNotes(),
                session.followUpRequired(),
                session.nextFollowUpDate(),
                session.prescriptions(),
                appendLog(session.interactionLogs(), "Patient joined teleconsultation session")
        ));

        auditLogger.log(resolveActor(), "JOIN_TELECONSULTATION", appointmentId, correlationId);
        return mapTeleconsultation(updated);
    }

    @Override
    public TeleconsultationResponse completeTeleconsultation(String appointmentId, CompleteTeleconsultationRequest request, String correlationId) {
        TeleconsultationSession session = teleconsultationRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new TeleconsultationSessionNotFoundException(appointmentId));

        enforcePatientScope(session.patientId());

        if (TELECONSULT_COMPLETED.equals(session.status())) {
            return mapTeleconsultation(session);
        }

        if (!(TELECONSULT_INITIATED.equals(session.status()) || TELECONSULT_IN_PROGRESS.equals(session.status()))) {
            throw new AppointmentNotEligibleException(appointmentId, "teleconsultation is not in completable state status=" + session.status());
        }

        String trimmedNotes = request.consultationNotes().trim();
        boolean followUpRequired = request.followUpRequired();
        List<String> prescriptions = request.prescriptions() == null ? List.of() :
                request.prescriptions().stream().filter(p -> p != null && !p.isBlank()).toList();
        String normalizedNextFollowUpDate = null;
        if (followUpRequired) {
            String requestedNextFollowUpDate = request.nextFollowUpDate();
            if (requestedNextFollowUpDate == null || requestedNextFollowUpDate.isBlank()) {
                throw new IllegalArgumentException("nextFollowUpDate is required when followUpRequired is true");
            }
            normalizedNextFollowUpDate = normaliseFollowUpDate(requestedNextFollowUpDate);
        }

        try {
            teleconsultationMedicalRecordAdapter.syncConsultationNotes(session, trimmedNotes, prescriptions, correlationId);
        } catch (RuntimeException ex) {
            teleconsultationRepository.save(new TeleconsultationSession(
                    session.id(),
                    session.appointmentId(),
                    session.patientId(),
                    session.providerId(),
                    session.status(),
                    session.doctorJoinUrl(),
                    session.patientJoinUrl(),
                    session.startedAt(),
                    session.joinedAt(),
                    session.completedAt(),
                    session.consultationNotes(),
                    session.followUpRequired(),
                    session.nextFollowUpDate(),
                    session.prescriptions(),
                    appendLog(session.interactionLogs(), "Teleconsultation completion failed due to network/integration issue")
            ));
            throw ex;
        }

        TeleconsultationSession completed = teleconsultationRepository.save(new TeleconsultationSession(
                session.id(),
                session.appointmentId(),
                session.patientId(),
                session.providerId(),
                TELECONSULT_COMPLETED,
                session.doctorJoinUrl(),
                session.patientJoinUrl(),
                session.startedAt(),
                session.joinedAt() == null ? now() : session.joinedAt(),
                now(),
                trimmedNotes,
                followUpRequired,
                normalizedNextFollowUpDate,
                prescriptions,
                appendLog(session.interactionLogs(), "Doctor completed teleconsultation and notes were recorded")
        ));

        // Mark the parent appointment as COMPLETED
        repository.findById(appointmentId).ifPresent(appt -> repository.save(new AppointmentRecord(
                appt.id(), "COMPLETED", appt.patientId(), appt.providerId(), appt.scheduledAt(), appt.channel())));

        messagingPort.publish(teleconsultationCompletedChannel, correlationId, new TeleconsultationCompletedEvent(
                completed.id(),
                completed.appointmentId(),
                completed.patientId(),
                completed.providerId(),
                completed.completedAt(),
                completed.followUpRequired(),
                completed.nextFollowUpDate()
        ));

        auditLogger.log(resolveActor(), "COMPLETE_TELECONSULTATION", appointmentId, correlationId);
        return mapTeleconsultation(completed);
    }

    public AppointmentResponse bookWithNotification(CreateAppointmentRequest request, String correlationId) {
        return bookAppointment(request, correlationId);
    }

    private AppointmentRecord getBookedAppointment(String appointmentId) {
        AppointmentRecord appointment = repository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment record not found: " + appointmentId));

        if (!BOOKED.equals(appointment.status())) {
            throw new AppointmentNotEligibleException(appointmentId, "appointment status must be BOOKED");
        }

        return appointment;
    }

    private void validateSecureTeleconsultBaseUrl() {
        if (secureTeleconsultBaseUrl == null || !secureTeleconsultBaseUrl.startsWith("https://")) {
            throw new InsecureSessionConfigurationException(secureTeleconsultBaseUrl);
        }
    }

    private String now() {
        return OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /**
     * Normalises a follow-up date value so it always becomes a full ISO-8601 datetime.
     * If the value is already a datetime (contains 'T') it is returned as-is.
     * A date-only value (yyyy-MM-dd) is suffixed with T09:00:00Z.
     */
    private String normaliseFollowUpDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.contains("T") ? trimmed : trimmed + "T09:00:00Z";
    }

    private String logEntry(String message) {
        return now() + " | " + message;
    }

    private List<String> appendLog(List<String> logs, String message) {
        List<String> updated = new ArrayList<>();
        if (logs != null) {
            updated.addAll(logs);
        }
        updated.add(logEntry(message));
        return updated;
    }

    private List<String> buildDaySlots(String date) {
        List<String> slots = new java.util.ArrayList<>();
        LocalTime slot = SLOT_START;
        while (slot.isBefore(SLOT_END_EXCLUSIVE)) {
            slots.add(date + "T" + slot + ":00Z");
            slot = slot.plusMinutes(30);
        }
        return slots;
    }

    private AppointmentResponse map(AppointmentRecord aggregate) {
        return new AppointmentResponse(
                aggregate.id(),
                aggregate.status(),
                aggregate.patientId(),
                aggregate.providerId(),
                aggregate.scheduledAt(),
                aggregate.channel()
        );
    }

    private TeleconsultationResponse mapTeleconsultation(TeleconsultationSession session) {
        return new TeleconsultationResponse(
                session.id(),
                session.appointmentId(),
                session.status(),
                session.doctorJoinUrl(),
                session.patientJoinUrl(),
                session.startedAt(),
                session.joinedAt(),
                session.completedAt(),
                session.consultationNotes(),
                session.followUpRequired(),
                session.nextFollowUpDate(),
                session.prescriptions(),
                session.interactionLogs()
        );
    }

    private void enforcePatientScope(String requestedPatientId) {
        if (!isPatientPrincipal()) {
            return;
        }

        String patientScope = patientScopeClaim().orElseThrow(() -> new AccessDeniedException("Patient scope violation"));
        if (requestedPatientId == null || !requestedPatientId.equalsIgnoreCase(patientScope)) {
            throw new AccessDeniedException("Patient scope violation");
        }
    }

    private boolean isPatientPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream().anyMatch(authority -> "ROLE_PATIENT".equals(authority.getAuthority()));
    }

    private Optional<String> patientScopeClaim() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            return Optional.empty();
        }

        String[] candidateClaims = {"patientId", "patient_id", "externalReference", "external_reference", "sub"};
        for (String candidate : candidateClaims) {
            String claimValue = jwtAuthenticationToken.getToken().getClaimAsString(candidate);
            if (claimValue != null && !claimValue.isBlank()) {
                return Optional.of(claimValue);
            }
        }
        return Optional.empty();
    }

    private String resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }
        if (authentication instanceof JwtAuthenticationToken jwt) {
            String sub = jwt.getToken().getClaimAsString("sub");
            if (sub != null && !sub.isBlank()) {
                return sub;
            }
        }
        return authentication.getName() != null ? authentication.getName() : "unknown";
    }
}
