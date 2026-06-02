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
                String correlationId = CorrelationIdHolder.get().orElse("n/a");
                return new StandardResponse<>(correlationId, service.recordConsent(request, correlationId));
            }

            @Operation(summary = "Get Consent resource")
            @GetMapping("/{id}")
            public StandardResponse<ConsentResponse> get(@PathVariable String id) {
                return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), service.getConsent(id));
            }


@Operation(summary = "List Consent resources")
@GetMapping
public StandardResponse<List<ConsentResponse>> list() {
    return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), service.listConsents());
}

@Operation(summary = "List consent history by patient and consent type")
@GetMapping("/history")
    public StandardResponse<List<ConsentResponse>> history(
            @RequestParam("patientId") String patientId,
            @RequestParam("consentType") String consentType) {
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
    return new StandardResponse<>(
            CorrelationIdHolder.get().orElse("n/a"),
            service.checkAccess(patientId, consentType)
    );
}
        }
