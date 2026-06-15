package com.healthcare.consent.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConsentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldFilterConsentListForPatientScope() throws Exception {
        mockMvc.perform(post("/consents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": "pat-consent-1001",
                                  "consentType": "GENERAL_CARE",
                                  "granted": true,
                                  "effectiveFrom": "2026-07-14T09:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/consents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": "pat-consent-2002",
                                  "consentType": "GENERAL_CARE",
                                  "granted": false,
                                  "effectiveFrom": "2026-07-14T09:05:00Z"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/consents").with(patientJwt("pat-consent-1001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].patientId").value("pat-consent-1001"));
    }

    @Test
    void shouldRejectConsentHistoryForPatientScopeMismatch() throws Exception {
        mockMvc.perform(get("/consents/history")
                        .param("patientId", "pat-consent-3001")
                        .param("consentType", "GENERAL_CARE")
                        .with(patientJwt("pat-consent-other")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowConsentHistoryForMatchingPatientScope() throws Exception {
        mockMvc.perform(post("/consents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": "pat-consent-4001",
                                  "consentType": "GENERAL_CARE",
                                  "granted": true,
                                  "effectiveFrom": "2026-07-14T10:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/consents/history")
                        .param("patientId", "pat-consent-4001")
                        .param("consentType", "GENERAL_CARE")
                        .with(patientJwt("pat-consent-4001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].patientId").value("pat-consent-4001"));
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
