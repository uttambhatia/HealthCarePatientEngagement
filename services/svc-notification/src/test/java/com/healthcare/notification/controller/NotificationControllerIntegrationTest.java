package com.healthcare.notification.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldSendNotificationWithSupportedChannel() throws Exception {
        String payload = """
                {
                  "recipientId": "pat-http-not-1001",
                  "channel": "SMS",
                  "templateId": "appt-reminder",
                  "message": "Your appointment is tomorrow at 09:30"
                }
                """;

        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DELIVERED"))
                .andExpect(jsonPath("$.data.channel").value("SMS"))
                .andExpect(jsonPath("$.data.deliveryAttempts").value(0));
    }

    @Test
    void shouldRejectUnsupportedNotificationChannel() throws Exception {
        String payload = """
                {
                  "recipientId": "pat-http-not-2001",
                  "channel": "FAX",
                  "templateId": "appt-reminder",
                  "message": "Unsupported channel test"
                }
                """;

        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_NOTIFICATION_CHANNEL"));
    }
}
