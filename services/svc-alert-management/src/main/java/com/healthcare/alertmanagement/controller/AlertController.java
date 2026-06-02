package com.healthcare.alertmanagement.controller;

        import com.healthcare.alertmanagement.dto.CreateAlertRequest;
        import com.healthcare.alertmanagement.dto.AlertResponse;
        import com.healthcare.alertmanagement.service.AlertApplicationService;
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
        @RequestMapping("/alerts")
        @Tag(name = "AlertManagement")
        public class AlertController {
            private final AlertApplicationService service;

            public AlertController(AlertApplicationService service) {
                this.service = service;
            }

            @Operation(summary = "Create AlertManagement resource")
            @PostMapping
            @ResponseStatus(HttpStatus.CREATED)
            public StandardResponse<AlertResponse> create(@Valid @RequestBody CreateAlertRequest request) {
                String correlationId = CorrelationIdHolder.get().orElse("n/a");
                return new StandardResponse<>(correlationId, service.triggerAlert(request, correlationId));
            }

            @Operation(summary = "Get AlertManagement resource")
            @GetMapping("/{id}")
            public StandardResponse<AlertResponse> get(@PathVariable("id") String id) {
                return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), service.getAlert(id));
            }


@Operation(summary = "List AlertManagement resources")
@GetMapping
public StandardResponse<List<AlertResponse>> list() {
    return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), service.listAlerts());
}
        }
