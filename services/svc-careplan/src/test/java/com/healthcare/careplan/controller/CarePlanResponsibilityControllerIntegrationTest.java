package com.healthcare.careplan.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
}
