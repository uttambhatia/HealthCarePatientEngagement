package com.healthcare.eventmessaging.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "platform.monitoring.anomaly.payload-max-length=50",
        "platform.monitoring.anomaly.keywords=ERROR,EXCEPTION"
})
@AutoConfigureMockMvc
class EventMessagingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldQueueAuditMessageWithImmutableMetadata() throws Exception {
        String payload = """
                {
                  "channel": "care-events",
                  "eventName": "AppointmentBooked",
                  "payload": "{\\\"appointmentId\\\":\\\"apt-2001\\\"}",
                  "messageType": "DOMAIN_EVENT"
                }
                """;

        mockMvc.perform(post("/servicebus/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andExpect(jsonPath("$.data.recordedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.integrityHash").isNotEmpty())
                .andExpect(jsonPath("$.data.anomalyReason").doesNotExist());
    }

    @Test
    void shouldMarkMessageAsAnomalyDetected() throws Exception {
        String payload = """
                {
                  "channel": "care-events",
                  "eventName": "TelemetryAnomaly",
                  "payload": "System ERROR threshold breach",
                  "messageType": "DOMAIN_EVENT"
                }
                """;

        mockMvc.perform(post("/servicebus/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ANOMALY_DETECTED"))
                .andExpect(jsonPath("$.data.anomalyReason").value("KEYWORD_MATCH:ERROR"));
    }
}
