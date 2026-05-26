package com.healthcare.gateway.integration;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DownstreamRegistryClient {
    public Map<String, String> routes() {
        return Map.of(
                "patients", "http://svc-patient:8081",
                "appointments", "http://svc-appointment:8082",
                "careplans", "http://svc-careplan:8083"
        );
    }
}
