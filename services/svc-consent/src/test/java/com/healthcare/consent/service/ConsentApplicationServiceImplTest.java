package com.healthcare.consent.service;

import com.healthcare.consent.dto.CreateConsentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ConsentApplicationServiceImplTest {
    @Autowired
    private ConsentApplicationService service;

    @Test
    void shouldVersionConsentAndEvaluateAccess() {
        var first = service.recordConsent(
                new CreateConsentRequest("pat-1001", "GENERAL_CARE", true, "2026-05-31T10:00:00Z"),
                "corr-200"
        );
        var second = service.recordConsent(
                new CreateConsentRequest("pat-1001", "GENERAL_CARE", false, "2026-05-31T10:10:00Z"),
                "corr-201"
        );

        assertThat(first.version()).isEqualTo(1);
        assertThat(second.version()).isEqualTo(2);

        var history = service.listConsentHistory("pat-1001", "GENERAL_CARE");
        assertThat(history).hasSize(2);
        assertThat(history.get(0).version()).isEqualTo(2);

        var access = service.checkAccess("pat-1001", "GENERAL_CARE");
        assertThat(access.accessAllowed()).isFalse();
        assertThat(access.reason()).isEqualTo("CONSENT_DENIED");

        var missing = service.checkAccess("pat-2002", "GENERAL_CARE");
        assertThat(missing.accessAllowed()).isFalse();
        assertThat(missing.reason()).isEqualTo("CONSENT_REQUIRED");
    }
}
