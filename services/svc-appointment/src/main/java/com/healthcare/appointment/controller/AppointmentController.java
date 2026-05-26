package com.healthcare.appointment.controller;

        import com.healthcare.appointment.dto.CreateAppointmentRequest;
        import com.healthcare.appointment.dto.AppointmentResponse;
        import com.healthcare.appointment.service.AppointmentApplicationService;
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
        @RequestMapping("/appointments")
        @Tag(name = "Appointment")
        public class AppointmentController {
            private final AppointmentApplicationService service;

            public AppointmentController(AppointmentApplicationService service) {
                this.service = service;
            }

            @Operation(summary = "Create Appointment resource")
            @PostMapping
            @ResponseStatus(HttpStatus.CREATED)
            public StandardResponse<AppointmentResponse> create(@Valid @RequestBody CreateAppointmentRequest request) {
                String correlationId = CorrelationIdHolder.get().orElse("n/a");
                return new StandardResponse<>(correlationId, service.bookAppointment(request, correlationId));
            }

            @Operation(summary = "Get Appointment resource")
            @GetMapping("/{id}")
            public StandardResponse<AppointmentResponse> get(@PathVariable String id) {
                return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), service.getAppointment(id));
            }


@Operation(summary = "List Appointment resources")
@GetMapping
public StandardResponse<List<AppointmentResponse>> list() {
    return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), service.listAppointments());
}
        }
