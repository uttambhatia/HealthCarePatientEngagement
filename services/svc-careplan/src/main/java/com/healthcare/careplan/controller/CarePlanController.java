package com.healthcare.careplan.controller;

        import com.healthcare.careplan.dto.CreateCarePlanRequest;
        import com.healthcare.careplan.dto.CarePlanResponse;
        import com.healthcare.careplan.service.CarePlanApplicationService;
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

            @Operation(summary = "Get CarePlan resource")
            @GetMapping("/{id}")
            public StandardResponse<CarePlanResponse> get(@PathVariable String id) {
                return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), service.getCarePlan(id));
            }


@Operation(summary = "List CarePlan resources")
@GetMapping
public StandardResponse<List<CarePlanResponse>> list() {
    return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), service.listCarePlans());
}
        }
