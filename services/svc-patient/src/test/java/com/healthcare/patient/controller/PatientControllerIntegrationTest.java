package com.healthcare.patient.controller;

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
class PatientControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldFilterPatientsForPatientScopeOnList() throws Exception {
        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalReference": "ext-pat-1001",
                                  "givenName": "Asha",
                                  "familyName": "Sharma",
                                  "birthDate": "1991-02-11",
                                  "email": "asha1001@example.com",
                                  "phone": "+919999000101",
                                  "demographics": "F"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalReference": "ext-pat-2002",
                                  "givenName": "Ravi",
                                  "familyName": "Verma",
                                  "birthDate": "1989-04-19",
                                  "email": "ravi2002@example.com",
                                  "phone": "+919999000202",
                                  "demographics": "M"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/patients").with(patientJwt("ext-pat-1001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].externalReference").value("ext-pat-1001"));
    }

    @Test
    void shouldRejectGetPatientWhenPatientScopeMismatches() throws Exception {
        String patientId = mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalReference": "ext-own-3001",
                                  "givenName": "Nina",
                                  "familyName": "Rao",
                                  "birthDate": "1994-06-21",
                                  "email": "nina3001@example.com",
                                  "phone": "+919999000303",
                                  "demographics": "F"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(get("/patients/{id}", patientId)
                        .with(patientJwt("ext-other-3999")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowGetPatientWhenPatientScopeMatchesExternalReference() throws Exception {
        String patientId = mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalReference": "ext-own-4001",
                                  "givenName": "Mohan",
                                  "familyName": "Das",
                                  "birthDate": "1987-10-09",
                                  "email": "mohan4001@example.com",
                                  "phone": "+919999000404",
                                  "demographics": "M"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(get("/patients/{id}", patientId)
                        .with(patientJwt("ext-own-4001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(patientId))
                .andExpect(jsonPath("$.data.externalReference").value("ext-own-4001"));
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
