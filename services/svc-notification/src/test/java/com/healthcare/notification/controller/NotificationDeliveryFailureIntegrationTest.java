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

@SpringBootTest(properties = {
        "ACS_INTEGRATION_BASE_URL=http://127.0.0.1:1",
        "platform.messaging.retryAttempts=1"
})
@AutoConfigureMockMvc
class NotificationDeliveryFailureIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnServiceUnavailableWhenAcsDeliveryFails() throws Exception {
        String payload = """
                {
                  "recipientId": "pat-http-not-3001",
                  "channel": "EMAIL",
                  "templateId": "careplan-update",
                  "message": "Your care plan has been updated"
                }
                """;

        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_DELIVERY_FAILED"));
    }
}
