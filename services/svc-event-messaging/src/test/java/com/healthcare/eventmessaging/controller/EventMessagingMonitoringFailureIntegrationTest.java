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
        "MONITORING_INTEGRATION_BASE_URL=http://127.0.0.1:1",
        "platform.messaging.retryAttempts=1"
})
@AutoConfigureMockMvc
class EventMessagingMonitoringFailureIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnServiceUnavailableWhenMonitoringDispatchFails() throws Exception {
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
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("MONITORING_DISPATCH_FAILED"));
    }
}
