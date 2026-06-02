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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    public StandardResponse<TelemetryResponse> get(@PathVariable String id) {
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
        return new StandardResponse<>(
                CorrelationIdHolder.get().orElse("n/a"),
                service.listTelemetryByPatient(patientId, metricType, startTime, endTime)
        );
    }

    @Operation(summary = "List available telemetry metric types")
    @GetMapping("/metric-types")
    public StandardResponse<List<String>> listMetricTypes(
            @RequestParam(value = "patientId", required = false) String patientId) {
        return new StandardResponse<>(
                CorrelationIdHolder.get().orElse("n/a"),
                service.listMetricTypes(patientId)
        );
    }
}
