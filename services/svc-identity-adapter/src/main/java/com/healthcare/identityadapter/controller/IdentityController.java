package com.healthcare.identityadapter.controller;

        import com.healthcare.identityadapter.dto.CreateIdentityRequest;
        import com.healthcare.identityadapter.dto.IdentityResponse;
        import com.healthcare.identityadapter.service.IdentityApplicationService;
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
        @RequestMapping("/identity/assertions")
        @Tag(name = "IdentityAdapter")
        public class IdentityController {
            private final IdentityApplicationService service;

            public IdentityController(IdentityApplicationService service) {
                this.service = service;
            }

            @Operation(summary = "Create IdentityAdapter resource")
            @PostMapping
            @ResponseStatus(HttpStatus.CREATED)
            public StandardResponse<IdentityResponse> create(@Valid @RequestBody CreateIdentityRequest request) {
                String correlationId = CorrelationIdHolder.get().orElse("n/a");
                return new StandardResponse<>(correlationId, service.validateIdentity(request, correlationId));
            }

            @Operation(summary = "Get IdentityAdapter resource")
            @GetMapping("/{id}")
            public StandardResponse<IdentityResponse> get(@PathVariable String id) {
                return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), service.getAssertion(id));
            }


@Operation(summary = "List IdentityAdapter resources")
@GetMapping
public StandardResponse<List<IdentityResponse>> list() {
    return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), service.listAssertions());
}
        }
