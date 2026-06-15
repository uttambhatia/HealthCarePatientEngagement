package com.healthcare.medicalrecord.controller;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MedicalRecordControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldUpdateMedicalRecordAndIncrementVersion() throws Exception {
        String createPayload = """
                {
                  "patientId": "pat-http-mr-1001",
                  "fhirResourceType": "Observation",
                  "resourceReference": "Observation/obs-http-1001",
                  "summary": "Initial observation captured"
                }
                """;

        String createdResponse = mockMvc.perform(post("/medical-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.version").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String recordId = createdResponse.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");

        String updatePayload = """
                {
                  "fhirResourceType": "Condition",
                  "resourceReference": "Condition/cond-http-1001",
                  "summary": "Condition updated after review",
                  "expectedVersion": 1
                }
                """;

        mockMvc.perform(put("/medical-records/{id}", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(recordId))
                .andExpect(jsonPath("$.data.fhirResourceType").value("Condition"))
                .andExpect(jsonPath("$.data.version").value(2));
    }

    @Test
    void shouldReturnConflictWhenMedicalRecordVersionMismatches() throws Exception {
        String createPayload = """
                {
                  "patientId": "pat-http-mr-2001",
                  "fhirResourceType": "Observation",
                  "resourceReference": "Observation/obs-http-2001",
                  "summary": "Initial snapshot"
                }
                """;

        String createdResponse = mockMvc.perform(post("/medical-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String recordId = createdResponse.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");

        String invalidUpdatePayload = """
                {
                  "fhirResourceType": "Observation",
                  "resourceReference": "Observation/obs-http-2002",
                  "summary": "Attempt stale update",
                  "expectedVersion": 5
                }
                """;

        mockMvc.perform(put("/medical-records/{id}", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidUpdatePayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
    }

    @Test
    void shouldFilterMedicalRecordsForPatientScopeOnList() throws Exception {
        mockMvc.perform(post("/medical-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": "pat-http-mr-3001",
                                  "fhirResourceType": "Observation",
                                  "resourceReference": "Observation/obs-http-3001",
                                  "summary": "Patient scoped record 1"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/medical-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": "pat-http-mr-4002",
                                  "fhirResourceType": "Observation",
                                  "resourceReference": "Observation/obs-http-4002",
                                  "summary": "Patient scoped record 2"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/medical-records").with(patientJwt("pat-http-mr-3001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].patientId").value("pat-http-mr-3001"));
    }

    @Test
    void shouldRejectGetMedicalRecordForPatientScopeMismatch() throws Exception {
        String createdResponse = mockMvc.perform(post("/medical-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": "pat-http-mr-5001",
                                  "fhirResourceType": "Observation",
                                  "resourceReference": "Observation/obs-http-5001",
                                  "summary": "Restricted record"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String recordId = createdResponse.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(get("/medical-records/{id}", recordId)
                        .with(patientJwt("pat-http-mr-other")))
                .andExpect(status().isForbidden());
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
