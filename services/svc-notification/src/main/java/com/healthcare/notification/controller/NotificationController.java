package com.healthcare.notification.controller;

        import com.healthcare.notification.dto.CreateNotificationRequest;
        import com.healthcare.notification.dto.NotificationResponse;
        import com.healthcare.notification.service.NotificationApplicationService;
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
        @RequestMapping("/notifications")
        @Tag(name = "Notification")
        public class NotificationController {
            private final NotificationApplicationService service;

            public NotificationController(NotificationApplicationService service) {
                this.service = service;
            }

            @Operation(summary = "Create Notification resource")
            @PostMapping
            @ResponseStatus(HttpStatus.CREATED)
            public StandardResponse<NotificationResponse> create(@Valid @RequestBody CreateNotificationRequest request) {
                String correlationId = CorrelationIdHolder.get().orElse("n/a");
                return new StandardResponse<>(correlationId, service.sendNotification(request, correlationId));
            }

            @Operation(summary = "Get Notification resource")
            @GetMapping("/{id}")
            public StandardResponse<NotificationResponse> get(@PathVariable("id") String id) {
                return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), service.getNotification(id));
            }


@Operation(summary = "List Notification resources")
@GetMapping
public StandardResponse<List<NotificationResponse>> list() {
    return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), service.listNotifications());
}
        }
