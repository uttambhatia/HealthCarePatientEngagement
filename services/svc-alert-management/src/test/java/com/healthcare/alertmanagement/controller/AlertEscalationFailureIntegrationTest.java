package com.healthcare.alertmanagement.controller;

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
        "ALERT_ESCALATION_BASE_URL=http://127.0.0.1:1",
        "platform.alert.rules.hr.max=100",
        "platform.messaging.retryAttempts=1"
})
@AutoConfigureMockMvc
class AlertEscalationFailureIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnServiceUnavailableWhenEscalationFails() throws Exception {
        String payload = """
                {
                  "patientId": "pat-http-alt-2001",
                                                                        "deviceId": "dev-http-alt-2001",
                  "severity": "HIGH",
                  "triggerType": "HEART_RATE",
                  "metricValue": "140",
                  "summary": "Escalation failure test"
                }
                """;

        mockMvc.perform(post("/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ALERT_ESCALATION_FAILED"));
    }
}
