package com.healthcare.platform.common.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StandardResponseTest {
    @Test
    void shouldExposeCorrelationId() {
        StandardResponse<String> response = new StandardResponse<>("corr-1", "ok");
        assertThat(response.correlationId()).isEqualTo("corr-1");
        assertThat(response.data()).isEqualTo("ok");
    }
}
