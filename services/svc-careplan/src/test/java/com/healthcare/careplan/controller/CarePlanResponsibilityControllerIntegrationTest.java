package com.healthcare.careplan.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest
@AutoConfigureMockMvc
class CarePlanResponsibilityControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldGetResponsibilityForExistingPatient() throws Exception {
        mockMvc.perform(post("/careplans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": "pat-resp-1001",
                                  "goal": "Remote monitoring ownership",
                                  "planStatus": "ACTIVE",
                                  "ownerId": "coord-500",
                                  "tasks": ["Daily telemetry review"]
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/careplans/responsibility/{patientId}", "pat-resp-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.patientId").value("pat-resp-1001"))
                .andExpect(jsonPath("$.data.ownerId").value("coord-500"))
                .andExpect(jsonPath("$.data.planStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.version").value(1));
    }

    @Test
    void shouldReturnNotFoundWhenResponsibilityIsMissing() throws Exception {
        mockMvc.perform(get("/careplans/responsibility/{patientId}", "pat-resp-missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

        @Test
        void shouldRejectResponsibilityAccessForPatientScopeMismatch() throws Exception {
      mockMvc.perform(post("/careplans")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
            {
              "patientId": "pat-resp-2001",
              "goal": "Mismatched scope access",
              "planStatus": "ACTIVE",
              "ownerId": "coord-501",
              "tasks": ["Task A"]
            }
            """))
        .andExpect(status().isCreated());

      mockMvc.perform(get("/careplans/responsibility/{patientId}", "pat-resp-2001")
          .with(patientJwt("pat-resp-other")))
        .andExpect(status().isForbidden());
        }

        @Test
        void shouldFilterCarePlansForPatientScopeOnList() throws Exception {
      mockMvc.perform(post("/careplans")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
            {
              "patientId": "pat-list-1001",
              "goal": "Patient list ownership 1",
              "planStatus": "ACTIVE",
              "ownerId": "coord-601",
              "tasks": ["Vitals review"]
            }
            """))
        .andExpect(status().isCreated());

      mockMvc.perform(post("/careplans")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
            {
              "patientId": "pat-list-2002",
              "goal": "Patient list ownership 2",
              "planStatus": "ACTIVE",
              "ownerId": "coord-602",
              "tasks": ["Medication adherence"]
            }
            """))
        .andExpect(status().isCreated());

      mockMvc.perform(get("/careplans").with(patientJwt("pat-list-1001")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].patientId").value("pat-list-1001"));
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
