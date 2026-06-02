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
        "REGISTERED_DEVICE_IDS=dev-22,dev-99"
})
@AutoConfigureMockMvc
class DeviceIngestionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldIngestValidDeviceSignal() throws Exception {
        String payload = """
                {
                  "deviceId": "dev-22",
                  "protocol": "MQTT",
                                                                        "payload": "{\\\"spo2\\\":97}",
                  "receivedAt": "2026-05-31T10:07:00Z"
                }
                """;

        mockMvc.perform(post("/devices/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("INGESTED"))
                .andExpect(jsonPath("$.data.deviceId").value("dev-22"))
                .andExpect(jsonPath("$.data.protocol").value("MQTT"));
    }

    @Test
    void shouldRejectInvalidTelemetryPayload() throws Exception {
        String payload = """
                {
                  "deviceId": "dev-22",
                  "protocol": "MQTT",
                  "payload": "not-json",
                  "receivedAt": "2026-05-31T10:07:00Z"
                }
                """;

        mockMvc.perform(post("/devices/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TELEMETRY_PAYLOAD"));
    }

    @Test
    void shouldRejectUnregisteredDevice() throws Exception {
        String payload = """
                {
                  "deviceId": "dev-404",
                  "protocol": "MQTT",
                                                                        "payload": "{\\\"hr\\\":72}",
                  "receivedAt": "2026-05-31T10:07:00Z"
                }
                """;

        mockMvc.perform(post("/devices/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DEVICE_NOT_REGISTERED"));
    }
}
