package com.healthcare.eventmessaging.controller;

        import com.healthcare.eventmessaging.dto.CreateServiceBusMessageRequest;
        import com.healthcare.eventmessaging.dto.ServiceBusMessageResponse;
        import com.healthcare.eventmessaging.service.EventMessagingApplicationService;
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
        @RequestMapping("/servicebus/messages")
        @Tag(name = "EventMessaging")
        public class EventMessagingController {
            private final EventMessagingApplicationService service;

            public EventMessagingController(EventMessagingApplicationService service) {
                this.service = service;
            }

            @Operation(summary = "Create EventMessaging resource")
            @PostMapping
            @ResponseStatus(HttpStatus.CREATED)
            public StandardResponse<ServiceBusMessageResponse> create(@Valid @RequestBody CreateServiceBusMessageRequest request) {
                String correlationId = CorrelationIdHolder.get().orElse("n/a");
                return new StandardResponse<>(correlationId, service.queueMessage(request, correlationId));
            }

            @Operation(summary = "Get EventMessaging resource")
            @GetMapping("/{id}")
            public StandardResponse<ServiceBusMessageResponse> get(@PathVariable("id") String id) {
                return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), service.getMessage(id));
            }


@Operation(summary = "List EventMessaging resources")
@GetMapping
public StandardResponse<List<ServiceBusMessageResponse>> list() {
    return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), service.listMessages());
}
        }
