package com.healthcare.medicalrecord.controller;

        import com.healthcare.medicalrecord.dto.CreateMedicalRecordRequest;
        import com.healthcare.medicalrecord.dto.MedicalRecordResponse;
        import com.healthcare.medicalrecord.service.MedicalRecordApplicationService;
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
        @RequestMapping("/medical-records")
        @Tag(name = "MedicalRecord")
        public class MedicalRecordController {
            private final MedicalRecordApplicationService service;

            public MedicalRecordController(MedicalRecordApplicationService service) {
                this.service = service;
            }

            @Operation(summary = "Create MedicalRecord resource")
            @PostMapping
            @ResponseStatus(HttpStatus.CREATED)
            public StandardResponse<MedicalRecordResponse> create(@Valid @RequestBody CreateMedicalRecordRequest request) {
                String correlationId = CorrelationIdHolder.get().orElse("n/a");
                return new StandardResponse<>(correlationId, service.syncMedicalRecord(request, correlationId));
            }

            @Operation(summary = "Get MedicalRecord resource")
            @GetMapping("/{id}")
            public StandardResponse<MedicalRecordResponse> get(@PathVariable String id) {
                return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), service.getMedicalRecord(id));
            }


@Operation(summary = "List MedicalRecord resources")
@GetMapping
public StandardResponse<List<MedicalRecordResponse>> list() {
    return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), service.listMedicalRecords());
}
        }
