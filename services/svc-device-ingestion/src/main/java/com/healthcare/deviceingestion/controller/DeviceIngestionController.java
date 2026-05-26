package com.healthcare.deviceingestion.controller;

        import com.healthcare.deviceingestion.dto.CreateDeviceIngestionRequest;
        import com.healthcare.deviceingestion.dto.DeviceIngestionResponse;
        import com.healthcare.deviceingestion.service.DeviceIngestionApplicationService;
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
        @RequestMapping("/devices/events")
        @Tag(name = "DeviceIngestion")
        public class DeviceIngestionController {
            private final DeviceIngestionApplicationService service;

            public DeviceIngestionController(DeviceIngestionApplicationService service) {
                this.service = service;
            }

            @Operation(summary = "Create DeviceIngestion resource")
            @PostMapping
            @ResponseStatus(HttpStatus.CREATED)
            public StandardResponse<DeviceIngestionResponse> create(@Valid @RequestBody CreateDeviceIngestionRequest request) {
                String correlationId = CorrelationIdHolder.get().orElse("n/a");
                return new StandardResponse<>(correlationId, service.ingestDeviceSignal(request, correlationId));
            }

            @Operation(summary = "Get DeviceIngestion resource")
            @GetMapping("/{id}")
            public StandardResponse<DeviceIngestionResponse> get(@PathVariable String id) {
                return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), service.getDeviceSignal(id));
            }


@Operation(summary = "List DeviceIngestion resources")
@GetMapping
public StandardResponse<List<DeviceIngestionResponse>> list() {
    return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), service.listDeviceSignals());
}
        }
