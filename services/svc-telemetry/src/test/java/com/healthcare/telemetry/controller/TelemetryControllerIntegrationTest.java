package com.healthcare.telemetry.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TelemetryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRejectTelemetryByPatientForScopeMismatch() throws Exception {
        mockMvc.perform(get("/telemetry/by-patient/{patientId}", "pat-tel-1001")
                        .with(patientJwt("pat-tel-other")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowTelemetryByPatientForMatchingScope() throws Exception {
        mockMvc.perform(get("/telemetry/by-patient/{patientId}", "pat-tel-1001")
                        .with(patientJwt("pat-tel-1001")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectMetricTypesFilterForScopeMismatch() throws Exception {
        mockMvc.perform(get("/telemetry/metric-types")
                        .param("patientId", "pat-tel-2002")
                        .with(patientJwt("pat-tel-other")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowMetricTypesFilterForMatchingScope() throws Exception {
        mockMvc.perform(get("/telemetry/metric-types")
                        .param("patientId", "pat-tel-2002")
                        .with(patientJwt("pat-tel-2002")))
                .andExpect(status().isOk());
    }

    private RequestPostProcessor patientJwt(String patientId) {
        return jwt()
                .jwt(jwt -> jwt
                        .claim("patientId", patientId)
                        .claim("sub", patientId)
                        .claim("roles", java.util.List.of("PATIENT")))
                .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"));
    }
}
