package com.healthcare.telemetry.controller;

import com.healthcare.telemetry.dto.CreateTelemetryRequest;
import com.healthcare.telemetry.dto.TelemetryResponse;
import com.healthcare.telemetry.service.TelemetryApplicationService;
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
@RequestMapping("/telemetry")
@Tag(name = "Telemetry")
public class TelemetryController {
    private final TelemetryApplicationService service;

    public TelemetryController(TelemetryApplicationService service) {
        this.service = service;
    }

    @Operation(summary = "Create Telemetry resource")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StandardResponse<TelemetryResponse> create(@Valid @RequestBody CreateTelemetryRequest request) {
        String correlationId = CorrelationIdHolder.get().orElse("n/a");
        return new StandardResponse<>(correlationId, service.recordTelemetry(request, correlationId));
    }

    @Operation(summary = "Get Telemetry resource")
    @GetMapping("/{id}")
    public StandardResponse<TelemetryResponse> get(@PathVariable("id") String id) {
        return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), service.getTelemetry(id));
    }

    @Operation(summary = "List Telemetry resources")
    @GetMapping
    public StandardResponse<List<TelemetryResponse>> list() {
        return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), service.listTelemetry());
    }

    @Operation(summary = "List Telemetry resources for patient")
    @GetMapping("/by-patient/{patientId}")
    public StandardResponse<List<TelemetryResponse>> listByPatient(
            @PathVariable("patientId") String patientId,
            @RequestParam(value = "metricType", required = false) String metricType,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime) {
        enforcePatientScope(patientId);
        return new StandardResponse<>(
                CorrelationIdHolder.get().orElse("n/a"),
                service.listTelemetryByPatient(patientId, metricType, startTime, endTime)
        );
    }

    @Operation(summary = "List available telemetry metric types")
    @GetMapping("/metric-types")
    public StandardResponse<List<String>> listMetricTypes(
            @RequestParam(value = "patientId", required = false) String patientId) {
        if (patientId != null && !patientId.isBlank()) {
            enforcePatientScope(patientId);
        }
        return new StandardResponse<>(
                CorrelationIdHolder.get().orElse("n/a"),
                service.listMetricTypes(patientId)
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
