package com.healthcare.appointment.service;

import com.healthcare.appointment.dto.CompleteTeleconsultationRequest;
import com.healthcare.appointment.dto.CreateAppointmentRequest;
import com.healthcare.appointment.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TeleconsultationWorkflowServiceTest {

    @Autowired
    private AppointmentApplicationService service;

    @Test
    void shouldCompleteTeleconsultationAndRecordNotes() {
        var appointment = service.bookAppointment(
                new CreateAppointmentRequest("pat-tel-1001", "prov-tel-44", "2026-07-01T11:00:00Z", "VIDEO"),
                "corr-tel-100"
        );

        var started = service.startTeleconsultation(appointment.id(), "corr-tel-101");
        assertThat(started.status()).isEqualTo("INITIATED");
        assertThat(started.doctorJoinUrl()).startsWith("https://");
        assertThat(started.interactionLogs()).isNotEmpty();

        var joined = service.joinTeleconsultation(appointment.id(), "corr-tel-102");
        assertThat(joined.status()).isEqualTo("IN_PROGRESS");
        assertThat(joined.joinedAt()).isNotBlank();

        var completed = service.completeTeleconsultation(
                appointment.id(),
            new CompleteTeleconsultationRequest(
                "Patient reports mild improvement; continue prescribed plan.",
                true,
                "2026-07-15T11:00:00Z"
            ),
                "corr-tel-103"
        );

        assertThat(completed.status()).isEqualTo("COMPLETED");
        assertThat(completed.completedAt()).isNotBlank();
        assertThat(completed.consultationNotes()).contains("mild improvement");
        assertThat(completed.followUpRequired()).isTrue();
        assertThat(completed.nextFollowUpDate()).isEqualTo("2026-07-15T11:00:00Z");
        assertThat(completed.interactionLogs().size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void shouldFailToStartTeleconsultationWhenAppointmentMissing() {
        assertThatThrownBy(() -> service.startTeleconsultation("missing-appointment", "corr-tel-200"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
