package com.healthcare.gateway.controller;

import com.healthcare.gateway.dto.DashboardResponse;
import com.healthcare.gateway.service.BffDashboardService;
import com.healthcare.platform.common.api.StandardResponse;
import com.healthcare.platform.common.observability.CorrelationIdHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bff")
public class BffController {
    private final BffDashboardService service;

    public BffController(BffDashboardService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    public StandardResponse<DashboardResponse> dashboard() {
        return new StandardResponse<>(CorrelationIdHolder.get().orElse("n/a"), service.getDashboard());
    }
}
