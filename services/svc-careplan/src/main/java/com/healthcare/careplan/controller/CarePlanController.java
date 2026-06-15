package com.healthcare.careplan.controller;

import com.healthcare.careplan.dto.CarePlanResponse;
import com.healthcare.careplan.dto.CarePlanResponsibilityResponse;
import com.healthcare.careplan.dto.CreateCarePlanRequest;
import com.healthcare.careplan.dto.UpdateCarePlanRequest;
import com.healthcare.careplan.service.CarePlanApplicationService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/careplans")
@Tag(name = "CarePlan")
public class CarePlanController {
    private final CarePlanApplicationService service;

    public CarePlanController(CarePlanApplicationService service) {
        this.service = service;
    }

    @Operation(summary = "Create CarePlan resource")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StandardResponse<CarePlanResponse> create(@Valid @RequestBody CreateCarePlanRequest request) {
        String correlationId = CorrelationIdHolder.get().orElse("n/a");
        return new StandardResponse<>(correlationId, service.createCarePlan(request, correlationId));
    }

    @Operation(summary = "Update CarePlan resource")
    @PutMapping("/{id}")
    public StandardResponse<CarePlanResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateCarePlanRequest request) {
        String correlationId = CorrelationIdHolder.get().orElse("n/a");
        return new StandardResponse<>(correlationId, service.updateCarePlan(id, request, correlationId));
    }

    @Operation(summary = "Get CarePlan resource")
    @GetMapping("/{id}")
    public StandardResponse<CarePlanResponse> get(@PathVariable String id) {
        CarePlanResponse response = service.getCarePlan(id);
        enforcePatientScope(response.patientId());
        return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), response);
    }

    @Operation(summary = "List CarePlan resources")
    @GetMapping
    public StandardResponse<List<CarePlanResponse>> list() {
        List<CarePlanResponse> responses = service.listCarePlans();
        if (isPatientPrincipal()) {
            String patientScope = patientScopeClaim().orElseThrow(this::forbidden);
            responses = responses.stream()
                    .filter(item -> item.patientId() != null && item.patientId().equalsIgnoreCase(patientScope))
                    .toList();
        }
        return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), responses);
    }

    @Operation(summary = "Get care coordinator responsibility for patient")
    @GetMapping("/responsibility/{patientId}")
    public StandardResponse<CarePlanResponsibilityResponse> responsibility(@PathVariable("patientId") String patientId) {
        enforcePatientScope(patientId);
        return new StandardResponse<>(
                CorrelationIdHolder.get().orElse("n/a"),
                service.getCarePlanResponsibility(patientId)
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
