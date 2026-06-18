package com.healthcare.identityadapter.controller;

import com.healthcare.identityadapter.dto.AcsNotificationRequest;
import com.healthcare.identityadapter.dto.AcsTeleconsultSessionRequest;
import com.healthcare.identityadapter.dto.AcsTeleconsultSessionResponse;
import com.healthcare.identityadapter.dto.AcsTeleconsultTokenRequest;
import com.healthcare.identityadapter.dto.AcsTeleconsultTokenResponse;
import com.healthcare.identityadapter.service.AcsIntegrationService;
import com.healthcare.platform.common.observability.CorrelationIdHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/acs")
@Tag(name = "AcsIntegration")
public class AcsIntegrationController {
    private final AcsIntegrationService acsIntegrationService;

    public AcsIntegrationController(AcsIntegrationService acsIntegrationService) {
        this.acsIntegrationService = acsIntegrationService;
    }

    @Operation(summary = "Accept notification dispatch request")
    @PostMapping("/notifications")
    public ResponseEntity<Void> notifications(@Valid @RequestBody AcsNotificationRequest request) {
        String correlationId = CorrelationIdHolder.get().orElse("n/a");
        acsIntegrationService.acceptNotification(request, correlationId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @Operation(summary = "Provision teleconsult session join URLs")
    @PostMapping("/teleconsult/sessions")
    public ResponseEntity<AcsTeleconsultSessionResponse> createTeleconsultSession(
            @Valid @RequestBody AcsTeleconsultSessionRequest request) {
        String correlationId = CorrelationIdHolder.get().orElse("n/a");
        AcsTeleconsultSessionResponse response = acsIntegrationService.createTeleconsultSession(request, correlationId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Issue teleconsult call client bootstrap token")
    @PostMapping("/teleconsult/token")
    public ResponseEntity<AcsTeleconsultTokenResponse> issueTeleconsultToken(
            @Valid @RequestBody AcsTeleconsultTokenRequest request) {
        String correlationId = CorrelationIdHolder.get().orElse("n/a");
        AcsTeleconsultTokenResponse response = acsIntegrationService.issueTeleconsultToken(request, correlationId);
        return ResponseEntity.ok(response);
    }
}