package com.healthcare.appointment.controller;

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
class AppointmentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnConflictWhenSlotAlreadyBooked() throws Exception {
        String bookingPayloadA = """
                {
                  \"patientId\": \"pat-http-1001\",
                  \"providerId\": \"prov-http-44\",
                  \"scheduledAt\": \"2026-06-10T11:00:00Z\",
                  \"channel\": \"VIDEO\"
                }
                """;

        String bookingPayloadB = """
                {
                  \"patientId\": \"pat-http-1002\",
                  \"providerId\": \"prov-http-44\",
                  \"scheduledAt\": \"2026-06-10T11:00:00Z\",
                  \"channel\": \"VIDEO\"
                }
                """;

        mockMvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingPayloadA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("BOOKED"));

        mockMvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingPayloadB))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_ALREADY_BOOKED"));
    }

    @Test
    void shouldReturnAvailableSlotsWithoutBookedSlot() throws Exception {
        String bookingPayload = """
                {
                  \"patientId\": \"pat-http-2001\",
                  \"providerId\": \"prov-http-55\",
                  \"scheduledAt\": \"2026-06-11T09:30:00Z\",
                  \"channel\": \"VIDEO\"
                }
                """;

        mockMvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingPayload))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/appointments/available-slots")
                        .param("providerId", "prov-http-55")
                        .param("date", "2026-06-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.providerId").value("prov-http-55"))
                .andExpect(jsonPath("$.data.date").value("2026-06-11"))
                .andExpect(jsonPath("$.data.availableSlots[?(@ == '2026-06-11T09:30:00Z')]").isEmpty())
                .andExpect(jsonPath("$.data.availableSlots.length()").value(15));
    }

    @Test
    void shouldRunTeleconsultationWorkflow() throws Exception {
        String bookingPayload = """
                {
                  "patientId": "pat-http-3001",
                  "providerId": "prov-http-66",
                  "scheduledAt": "2026-06-20T10:30:00Z",
                  "channel": "VIDEO"
                }
                """;

        String appointmentId = mockMvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("BOOKED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String extractedId = appointmentId.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(post("/appointments/{id}/teleconsult/start", extractedId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INITIATED"));

        mockMvc.perform(post("/appointments/{id}/teleconsult/join", extractedId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        String completePayload = """
                {
                  "consultationNotes": "Consultation completed and medication plan reviewed."
                }
                """;

        mockMvc.perform(post("/appointments/{id}/teleconsult/complete", extractedId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(completePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.consultationNotes").value("Consultation completed and medication plan reviewed."));
    }

    @Test
    void shouldReturnNotFoundWhenCompletingTeleconsultWithoutSession() throws Exception {
        String completePayload = """
                {
                  "consultationNotes": "Attempt complete without session"
                }
                """;

        mockMvc.perform(post("/appointments/{id}/teleconsult/complete", "missing-appointment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(completePayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TELECONSULTATION_NOT_FOUND"));
    }

    @Test
    void shouldReturnBadRequestWhenTeleconsultCompletionNotesBlank() throws Exception {
        String bookingPayload = """
                {
                  "patientId": "pat-http-3002",
                  "providerId": "prov-http-67",
                  "scheduledAt": "2026-06-21T10:30:00Z",
                  "channel": "VIDEO"
                }
                """;

        String appointmentId = mockMvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingPayload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(post("/appointments/{id}/teleconsult/start", appointmentId))
                .andExpect(status().isOk());

        String invalidCompletePayload = """
                {
                  "consultationNotes": "   "
                }
                """;

        mockMvc.perform(post("/appointments/{id}/teleconsult/complete", appointmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidCompletePayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldFilterAppointmentsForPatientScopeOnList() throws Exception {
        mockMvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": "pat-scope-1001",
                                  "providerId": "prov-scope-1",
                                  "scheduledAt": "2026-07-11T09:00:00Z",
                                  "channel": "VIDEO"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": "pat-scope-2002",
                                  "providerId": "prov-scope-2",
                                  "scheduledAt": "2026-07-11T09:30:00Z",
                                  "channel": "VIDEO"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/appointments").with(patientJwt("pat-scope-1001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].patientId").value("pat-scope-1001"));
    }

    @Test
    void shouldRejectTeleconsultJoinWhenPatientScopeMismatches() throws Exception {
        String appointmentId = mockMvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": "pat-owner-3001",
                                  "providerId": "prov-owner-88",
                                  "scheduledAt": "2026-07-12T10:00:00Z",
                                  "channel": "VIDEO"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(post("/appointments/{id}/teleconsult/start", appointmentId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/appointments/{id}/teleconsult/join", appointmentId)
                        .with(patientJwt("pat-other-3999")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
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
