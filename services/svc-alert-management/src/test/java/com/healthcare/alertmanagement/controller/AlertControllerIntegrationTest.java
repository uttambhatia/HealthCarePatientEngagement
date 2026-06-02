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
        "platform.alert.rules.hr.max=100"
})
@AutoConfigureMockMvc
class AlertControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateOpenAlertWhenThresholdBreached() throws Exception {
        String payload = """
                {
                  "patientId": "pat-http-alt-1001",
                                                                        "deviceId": "dev-http-alt-1001",
                  "severity": "HIGH",
                  "triggerType": "HEART_RATE",
                  "metricValue": "124",
                  "summary": "Heart rate threshold breached"
                }
                """;

        mockMvc.perform(post("/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.deviceId").value("dev-http-alt-1001"))
                .andExpect(jsonPath("$.data.triggerType").value("HEART_RATE"));
    }

    @Test
    void shouldSuppressAlertWhenThresholdNotBreached() throws Exception {
        String payload = """
                {
                  "patientId": "pat-http-alt-1002",
                                                                        "deviceId": "dev-http-alt-1002",
                  "severity": "MEDIUM",
                  "triggerType": "HEART_RATE",
                  "metricValue": "88",
                  "summary": "Heart rate within threshold"
                }
                """;

        mockMvc.perform(post("/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("SUPPRESSED"))
                .andExpect(jsonPath("$.data.deviceId").value("dev-http-alt-1002"))
                .andExpect(jsonPath("$.data.triggerType").value("HEART_RATE"));
    }

    @Test
    void shouldRejectNonNumericMetricValue() throws Exception {
        String payload = """
                {
                  "patientId": "pat-http-alt-1003",
                                                                        "deviceId": "dev-http-alt-1003",
                  "severity": "MEDIUM",
                  "triggerType": "GLUCOSE",
                  "metricValue": "not-a-number",
                  "summary": "Invalid metric"
                }
                """;

        mockMvc.perform(post("/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ALERT_METRIC"));
    }
}
