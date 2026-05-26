package com.healthcare.gateway.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BffDashboardServiceImplTest {
    @Autowired
    private BffDashboardService service;

    @Test
    void shouldExposeDashboardModules() {
        assertThat(service.getDashboard().routes()).isNotEmpty();
    }
}
