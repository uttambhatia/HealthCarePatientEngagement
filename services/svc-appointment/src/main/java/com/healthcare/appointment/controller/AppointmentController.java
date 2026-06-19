package com.healthcare.appointment.controller;

import com.healthcare.appointment.dto.AppointmentResponse;
import com.healthcare.appointment.dto.AvailableSlotResponse;
import com.healthcare.appointment.dto.CompleteTeleconsultationRequest;
import com.healthcare.appointment.dto.CreateAppointmentRequest;
import com.healthcare.appointment.dto.TeleconsultationResponse;
import com.healthcare.appointment.service.AppointmentApplicationService;
import com.healthcare.platform.common.api.StandardResponse;
import com.healthcare.platform.common.observability.CorrelationIdHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/appointments")
@Tag(name = "Appointment")
public class AppointmentController {
    private static final Logger logger = LoggerFactory.getLogger(AppointmentController.class);
    private final AppointmentApplicationService service;

    public AppointmentController(AppointmentApplicationService service) {
        this.service = service;
    }

    @Operation(summary = "Create Appointment resource")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StandardResponse<AppointmentResponse> create(@Valid @RequestBody CreateAppointmentRequest request) {
        CreateAppointmentRequest effective = request;
        if (isPatientPrincipal()) {
            String scopedPatientId = primaryPatientScopeClaim().orElseThrow(this::forbidden);
            if (!scopedPatientId.equalsIgnoreCase(request.patientId())) {
                effective = new CreateAppointmentRequest(scopedPatientId, request.providerId(), request.scheduledAt(), request.channel());
            }
        }
        String correlationId = CorrelationIdHolder.get().orElse("n/a");
        return new StandardResponse<>(correlationId, service.bookAppointment(effective, correlationId));
    }

    @Operation(summary = "Get Appointment resource")
    @GetMapping("/{id}")
    public StandardResponse<AppointmentResponse> get(@PathVariable String id) {
        AppointmentResponse response = service.getAppointment(id);
        enforcePatientScope(response.patientId());
        return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), response);
    }

    @Operation(summary = "List Appointment resources")
    @GetMapping
    public StandardResponse<List<AppointmentResponse>> list() {
        List<AppointmentResponse> responses = service.listAppointments();
        if (isPatientPrincipal()) {
            Set<String> patientScopes = patientMatchClaims();
            if (patientScopes.isEmpty()) {
                throw forbidden();
            }
            // Debug: Log extracted scopes and available patient IDs
            String existingPatientIds = responses.stream()
                    .map(AppointmentResponse::patientId)
                    .filter(id -> id != null)
                    .distinct()
                    .collect(Collectors.joining(", "));
            logger.info("Patient list request: scopes={}, existingPatientIds={}", patientScopes, existingPatientIds);
            
            responses = responses.stream()
                    .filter(item -> item.patientId() != null && matchesAnyScope(item.patientId(), patientScopes))
                    .toList();
        }
        return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), responses);
    }

    @Operation(summary = "List available slots for a provider on a date")
    @GetMapping("/available-slots")
    public StandardResponse<AvailableSlotResponse> availableSlots(
            @RequestParam("providerId") String providerId,
            @RequestParam("date") String date) {
        return new StandardResponse<>(
                CorrelationIdHolder.get().orElse("n/a"),
                service.listAvailableSlots(providerId, date)
        );
    }

    @Operation(summary = "Start teleconsultation session")
    @PostMapping("/{id}/teleconsult/start")
    public StandardResponse<TeleconsultationResponse> startTeleconsultation(@PathVariable("id") String id) {
        String correlationId = CorrelationIdHolder.get().orElse("n/a");
        return new StandardResponse<>(correlationId, service.startTeleconsultation(id, correlationId));
    }

    @Operation(summary = "Patient joins teleconsultation session")
    @PostMapping("/{id}/teleconsult/join")
    public StandardResponse<TeleconsultationResponse> joinTeleconsultation(@PathVariable("id") String id) {
        String correlationId = CorrelationIdHolder.get().orElse("n/a");
        return new StandardResponse<>(correlationId, service.joinTeleconsultation(id, correlationId));
    }

    @Operation(summary = "Complete teleconsultation and record notes")
    @PostMapping("/{id}/teleconsult/complete")
    public StandardResponse<TeleconsultationResponse> completeTeleconsultation(
            @PathVariable("id") String id,
            @Valid @RequestBody CompleteTeleconsultationRequest request) {
        String correlationId = CorrelationIdHolder.get().orElse("n/a");
        return new StandardResponse<>(correlationId, service.completeTeleconsultation(id, request, correlationId));
    }

    private boolean isPatientPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream().anyMatch(authority -> "ROLE_PATIENT".equals(authority.getAuthority()));
    }

    private Set<String> patientScopeClaims() {
        return canonicalPatientClaims();
    }

    private Set<String> canonicalPatientClaims() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            return Set.of();
        }

        Set<String> values = new LinkedHashSet<>();
        String[] candidateClaims = {"patientId", "patient_id", "externalReference", "external_reference", "sub"};
        for (String candidate : candidateClaims) {
            String claimValue = jwtAuthenticationToken.getToken().getClaimAsString(candidate);
            if (claimValue != null && !claimValue.isBlank()) {
                values.add(claimValue);
            }
        }
        return values;
    }

    private Set<String> patientMatchClaims() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            return Set.of();
        }

        Set<String> values = new LinkedHashSet<>(canonicalPatientClaims());
        String[] identityClaims = {"preferred_username", "upn", "email", "unique_name", "sub"};
        for (String candidate : identityClaims) {
            String claimValue = jwtAuthenticationToken.getToken().getClaimAsString(candidate);
            addMatchVariants(values, claimValue);
        }
        return values;
    }

    private void addMatchVariants(Set<String> values, String claimValue) {
        if (claimValue == null) {
            return;
        }
        String trimmed = claimValue.trim();
        if (trimmed.isBlank()) {
            return;
        }

        values.add(trimmed);
        int atIndex = trimmed.indexOf('@');
        if (atIndex > 0) {
            String localPart = trimmed.substring(0, atIndex);
            if (!localPart.isBlank()) {
                values.add(localPart);
            }
        }
    }

    private Optional<String> primaryPatientScopeClaim() {
        Set<String> scopes = patientScopeClaims();
        if (scopes.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(scopes.iterator().next());
    }

    private void enforcePatientScope(String requestedPatientId) {
        if (!isPatientPrincipal()) {
            return;
        }
        Set<String> patientScopes = patientMatchClaims();
        if (requestedPatientId == null || patientScopes.isEmpty() || !matchesAnyScope(requestedPatientId, patientScopes)) {
            throw forbidden();
        }
    }

    private boolean matchesAnyScope(String patientId, Set<String> scopes) {
        return scopes.stream().anyMatch(scope -> scope.equalsIgnoreCase(patientId));
    }

    private ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Patient scope violation");
    }
}
