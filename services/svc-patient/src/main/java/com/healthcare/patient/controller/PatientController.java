package com.healthcare.patient.controller;

import com.healthcare.patient.dto.CreatePatientRequest;
import com.healthcare.patient.dto.PatientResponse;
import com.healthcare.patient.service.PatientApplicationService;
import com.healthcare.platform.common.api.StandardResponse;
import com.healthcare.platform.common.observability.CorrelationIdHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/patients")
@Tag(name = "Patient")
public class PatientController {
    private final PatientApplicationService service;

    public PatientController(PatientApplicationService service) {
        this.service = service;
    }

    @Operation(summary = "Create Patient resource")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StandardResponse<PatientResponse> create(@Valid @RequestBody CreatePatientRequest request) {
        enforcePatientScope(request.externalReference());
        String correlationId = CorrelationIdHolder.get().orElse("n/a");
        return new StandardResponse<>(correlationId, service.registerPatient(request, correlationId));
    }

    @Operation(summary = "Get Patient resource")
    @GetMapping("/{id}")
    public StandardResponse<PatientResponse> get(@PathVariable("id") String id) {
        PatientResponse response = service.getPatient(id);
        if (isPatientPrincipal()) {
            String patientScope = patientScopeClaim().orElseThrow(this::forbidden);
            boolean matchesInternalId = response.id() != null && response.id().equalsIgnoreCase(patientScope);
            boolean matchesExternalReference = response.externalReference() != null && response.externalReference().equalsIgnoreCase(patientScope);
            if (!matchesInternalId && !matchesExternalReference) {
                throw forbidden();
            }
        }
        return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), response);
    }

    @Operation(summary = "List Patient resources")
    @GetMapping
    public StandardResponse<List<PatientResponse>> list() {
        List<PatientResponse> responses = service.listPatients();
        if (isPatientPrincipal()) {
            String patientScope = patientScopeClaim().orElseThrow(this::forbidden);
            responses = responses.stream()
                    .filter(item -> matchesPatientScope(item, patientScope))
                    .toList();
        }
        return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), responses);
    }

    @Operation(summary = "Approve pending patient registration")
    @PatchMapping("/{id}/approval/approve")
    public StandardResponse<PatientResponse> approve(@PathVariable("id") String id, Authentication authentication) {
        String correlationId = CorrelationIdHolder.get().orElse("n/a");
        return new StandardResponse<>(correlationId, service.approveRegistration(id, resolveActor(authentication), correlationId));
    }

    @Operation(summary = "Reject pending patient registration")
    @PatchMapping("/{id}/approval/reject")
    public StandardResponse<PatientResponse> reject(@PathVariable("id") String id, Authentication authentication) {
        String correlationId = CorrelationIdHolder.get().orElse("n/a");
        return new StandardResponse<>(correlationId, service.rejectRegistration(id, resolveActor(authentication), correlationId));
    }

    @Operation(summary = "Resend patient registration notification")
    @PostMapping("/{id}/notifications/resend")
    public StandardResponse<PatientResponse> resend(@PathVariable("id") String id) {
        String correlationId = CorrelationIdHolder.get().orElse("n/a");
        return new StandardResponse<>(correlationId, service.resendRegistrationNotification(id, correlationId));
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

    private void enforcePatientScope(String requestedPatientId) {
        if (!isPatientPrincipal()) {
            return;
        }
        String patientScope = patientScopeClaim().orElseThrow(this::forbidden);
        if (requestedPatientId == null || !requestedPatientId.equalsIgnoreCase(patientScope)) {
            throw forbidden();
        }
    }

    private boolean matchesPatientScope(PatientResponse response, String patientScope) {
        return (response.id() != null && response.id().equalsIgnoreCase(patientScope))
                || (response.externalReference() != null && response.externalReference().equalsIgnoreCase(patientScope));
    }

    private ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Patient scope violation");
    }

    private String resolveActor(Authentication authentication) {
        if (authentication == null) {
            return "unknown";
        }
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            String[] claimCandidates = {"preferred_username", "upn", "email", "name", "sub"};
            for (String claim : claimCandidates) {
                String value = jwtAuthenticationToken.getToken().getClaimAsString(claim);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        String name = authentication.getName();
        return (name == null || name.isBlank()) ? "unknown" : name;
    }
}
