package com.healthcare.consent.controller;

        import com.healthcare.consent.dto.CreateConsentRequest;
        import com.healthcare.consent.dto.ConsentAccessResponse;
        import com.healthcare.consent.dto.ConsentResponse;
        import com.healthcare.consent.service.ConsentApplicationService;
        import com.healthcare.platform.common.api.StandardResponse;
        import com.healthcare.platform.common.observability.CorrelationIdHolder;
        import io.swagger.v3.oas.annotations.Operation;
        import io.swagger.v3.oas.annotations.tags.Tag;
        import jakarta.validation.Valid;
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

        import java.util.List;
        import java.util.Optional;

        @RestController
        @RequestMapping("/consents")
        @Tag(name = "Consent")
        public class ConsentController {
            private final ConsentApplicationService service;

            public ConsentController(ConsentApplicationService service) {
                this.service = service;
            }

            @Operation(summary = "Create Consent resource")
            @PostMapping
            @ResponseStatus(HttpStatus.CREATED)
            public StandardResponse<ConsentResponse> create(@Valid @RequestBody CreateConsentRequest request) {
                enforcePatientScope(request.patientId());
                String correlationId = CorrelationIdHolder.get().orElse("n/a");
                return new StandardResponse<>(correlationId, service.recordConsent(request, correlationId));
            }

            @Operation(summary = "Get Consent resource")
            @GetMapping("/{id}")
            public StandardResponse<ConsentResponse> get(@PathVariable("id") String id) {
                ConsentResponse response = service.getConsent(id);
                enforcePatientScope(response.patientId());
                return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), response);
            }


@Operation(summary = "List Consent resources")
@GetMapping
public StandardResponse<List<ConsentResponse>> list() {
    List<ConsentResponse> responses = service.listConsents();
    if (isPatientPrincipal()) {
        String patientScope = patientScopeClaim().orElseThrow(this::forbidden);
        responses = responses.stream()
                .filter(item -> item.patientId() != null && item.patientId().equalsIgnoreCase(patientScope))
                .toList();
    }
    return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), responses);
}

@Operation(summary = "List consent history by patient and consent type")
@GetMapping("/history")
    public StandardResponse<List<ConsentResponse>> history(
            @RequestParam("patientId") String patientId,
            @RequestParam("consentType") String consentType) {
        enforcePatientScope(patientId);
    return new StandardResponse<>(
            CorrelationIdHolder.get().orElse("n/a"),
            service.listConsentHistory(patientId, consentType)
    );
}

@Operation(summary = "Check whether consent allows access for a patient")
@GetMapping("/check-access")
    public StandardResponse<ConsentAccessResponse> checkAccess(
            @RequestParam("patientId") String patientId,
            @RequestParam("consentType") String consentType) {
    enforcePatientScope(patientId);
    return new StandardResponse<>(
            CorrelationIdHolder.get().orElse("n/a"),
            service.checkAccess(patientId, consentType)
    );
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

private ResponseStatusException forbidden() {
    return new ResponseStatusException(HttpStatus.FORBIDDEN, "Patient scope violation");
}
        }
