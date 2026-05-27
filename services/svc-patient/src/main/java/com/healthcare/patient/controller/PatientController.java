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
        import org.springframework.web.bind.annotation.GetMapping;
        import org.springframework.web.bind.annotation.PathVariable;
        import org.springframework.web.bind.annotation.PostMapping;
        import org.springframework.web.bind.annotation.RequestBody;
        import org.springframework.web.bind.annotation.RequestMapping;
        import org.springframework.web.bind.annotation.ResponseStatus;
        import org.springframework.web.bind.annotation.RestController;

        import java.util.List;

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
                String correlationId = CorrelationIdHolder.get().orElse("n/a");
                return new StandardResponse<>(correlationId, service.registerPatient(request, correlationId));
            }

            @Operation(summary = "Get Patient resource")
            @GetMapping("/{id}")
            public StandardResponse<PatientResponse> get(@PathVariable String id) {
                return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), service.getPatient(id));
            }


@Operation(summary = "List Patient resources")
@GetMapping
public StandardResponse<List<PatientResponse>> list() {
    return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), service.listPatients());
}
        }
