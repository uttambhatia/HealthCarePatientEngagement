package com.healthcare.deviceingestion.controller;

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
        "REGISTERED_DEVICE_IDS=dev-22",
        "TELEMETRY_INTEGRATION_BASE_URL=http://127.0.0.1:1",
        "platform.messaging.retryAttempts=1"
})
@AutoConfigureMockMvc
class DeviceIngestionForwardingFailureIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnServiceUnavailableWhenTelemetryForwardingFails() throws Exception {
        String payload = """
                {
                  "deviceId": "dev-22",
                  "protocol": "MQTT",
                                                                        "payload": "{\\\"glucose\\\":105}",
                  "receivedAt": "2026-05-31T10:07:00Z"
                }
                """;

        mockMvc.perform(post("/devices/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("TELEMETRY_FORWARDING_FAILED"));
    }
}
