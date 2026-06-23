package com.healthcare.alertmanagement.event;

import com.healthcare.alertmanagement.dto.AlertResponse;
import com.healthcare.alertmanagement.service.AlertApplicationService;
import com.healthcare.platform.common.event.MessageEnvelope;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "platform.alert.teleconsult.critical-keywords=critical|urgent|emergency|severe|worsening|deteriorating|abnormal"
})
class TeleconsultationAlertConsumerTest {

    @Autowired
    private TeleconsultationAlertConsumer consumer;

    @Autowired
    private AlertApplicationService alertApplicationService;

    @Test
    void shouldTriggerCriticalFindingsAlertWhenNoteContainsKeyword() {
        TeleconsultationCompletedEvent event = new TeleconsultationCompletedEvent(
                "sess-crit-1", "apt-crit-1", "pat-crit-1", "prov-44",
                "2026-06-23T10:00:00Z", true, "2026-07-01T09:00:00Z",
                "Patient condition is critical and requires urgent follow-up"
        );

        consumer.handle(new MessageEnvelope<>("corr-crit-1", "TeleconsultationCompletedEvent", OffsetDateTime.now(), event));

        List<AlertResponse> alerts = alertApplicationService.listAlerts();
        assertThat(alerts)
                .anySatisfy(alert -> {
                    assertThat(alert.patientId()).isEqualTo("pat-crit-1");
                    assertThat(alert.triggerType()).isEqualTo("TELECONSULT_CRITICAL_FINDINGS");
                    assertThat(alert.severity()).isEqualTo("HIGH");
                });
    }

    @Test
    void shouldNotTriggerCriticalFindingsAlertWhenNoteHasNoKeyword() {
        long countBefore = alertApplicationService.listAlerts().stream()
                .filter(a -> "TELECONSULT_CRITICAL_FINDINGS".equals(a.triggerType())).count();

        TeleconsultationCompletedEvent event = new TeleconsultationCompletedEvent(
                "sess-noncrit-1", "apt-noncrit-1", "pat-noncrit-1", "prov-44",
                "2026-06-23T10:01:00Z", false, null,
                "Patient recovering well. Continue current medication."
        );

        consumer.handle(new MessageEnvelope<>("corr-noncrit-1", "TeleconsultationCompletedEvent", OffsetDateTime.now(), event));

        long countAfter = alertApplicationService.listAlerts().stream()
                .filter(a -> "TELECONSULT_CRITICAL_FINDINGS".equals(a.triggerType())
                          && "pat-noncrit-1".equals(a.patientId())).count();
        assertThat(countAfter).isZero();
    }

    @Test
    void shouldTriggerIncompleteFollowUpAlertWhenFollowUpRequiredButNoDatProvided() {
        TeleconsultationCompletedEvent event = new TeleconsultationCompletedEvent(
                "sess-nofollowup-1", "apt-nofollowup-1", "pat-nofollowup-1", "prov-44",
                "2026-06-23T10:02:00Z", true, null,
                "Patient recovering well."
        );

        consumer.handle(new MessageEnvelope<>("corr-nofollowup-1", "TeleconsultationCompletedEvent", OffsetDateTime.now(), event));

        List<AlertResponse> alerts = alertApplicationService.listAlerts();
        assertThat(alerts)
                .anySatisfy(alert -> {
                    assertThat(alert.patientId()).isEqualTo("pat-nofollowup-1");
                    assertThat(alert.triggerType()).isEqualTo("TELECONSULT_INCOMPLETE_FOLLOWUP");
                    assertThat(alert.severity()).isEqualTo("WARNING");
                });
    }

    @Test
    void shouldTriggerBothCriticalAndIncompleteFollowUpWhenBothConditionsMet() {
        TeleconsultationCompletedEvent event = new TeleconsultationCompletedEvent(
                "sess-both-1", "apt-both-1", "pat-both-1", "prov-44",
                "2026-06-23T10:03:00Z", true, null,
                "Patient deteriorating. Abnormal vitals detected."
        );

        consumer.handle(new MessageEnvelope<>("corr-both-1", "TeleconsultationCompletedEvent", OffsetDateTime.now(), event));

        List<AlertResponse> alerts = alertApplicationService.listAlerts();
        long criticalCount = alerts.stream()
                .filter(a -> "TELECONSULT_CRITICAL_FINDINGS".equals(a.triggerType()) && "pat-both-1".equals(a.patientId()))
                .count();
        long incompleteCount = alerts.stream()
                .filter(a -> "TELECONSULT_INCOMPLETE_FOLLOWUP".equals(a.triggerType()) && "pat-both-1".equals(a.patientId()))
                .count();

        assertThat(criticalCount).isGreaterThanOrEqualTo(1);
        assertThat(incompleteCount).isGreaterThanOrEqualTo(1);
    }
}
