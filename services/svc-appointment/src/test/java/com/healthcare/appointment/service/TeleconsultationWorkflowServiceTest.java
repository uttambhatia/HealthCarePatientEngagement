package com.healthcare.appointment.service;

import com.healthcare.appointment.dto.CompleteTeleconsultationRequest;
import com.healthcare.appointment.dto.CreateAppointmentRequest;
import com.healthcare.appointment.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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
                "2026-07-15T11:00:00Z",
                null
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

    @Test
    void shouldAllowPatientToJoinAndCompleteOwnTeleconsultation() {
        var appointment = service.bookAppointment(
                new CreateAppointmentRequest("pat-own-1001", "prov-tel-45", "2026-07-02T09:30:00Z", "VIDEO"),
                "corr-tel-301"
        );
        service.startTeleconsultation(appointment.id(), "corr-tel-302");

        withPatientScope("pat-own-1001", () -> {
            var joined = service.joinTeleconsultation(appointment.id(), "corr-tel-303");
            assertThat(joined.status()).isIn("IN_PROGRESS", "COMPLETED");

            var completed = service.completeTeleconsultation(
                    appointment.id(),
                    new CompleteTeleconsultationRequest("Own patient session notes", false, null, null),
                    "corr-tel-304"
            );
            assertThat(completed.status()).isEqualTo("COMPLETED");
        });
    }

    @Test
    void shouldRejectPatientScopeMismatchOnJoinAndComplete() {
        var appointment = service.bookAppointment(
                new CreateAppointmentRequest("pat-own-2001", "prov-tel-46", "2026-07-03T10:00:00Z", "VIDEO"),
                "corr-tel-401"
        );
        service.startTeleconsultation(appointment.id(), "corr-tel-402");

        withPatientScope("pat-other-9999", () -> {
            assertThatThrownBy(() -> service.joinTeleconsultation(appointment.id(), "corr-tel-403"))
                    .isInstanceOf(AccessDeniedException.class);

            assertThatThrownBy(() -> service.completeTeleconsultation(
                    appointment.id(),
                    new CompleteTeleconsultationRequest("Should fail", false, null, null),
                    "corr-tel-404"
            )).isInstanceOf(AccessDeniedException.class);
        });
    }

    private void withPatientScope(String patientId, Runnable action) {
        Jwt jwt = new Jwt(
                "token-value",
                Instant.now().minusSeconds(30),
                Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                Map.of("patientId", patientId, "sub", patientId, "roles", List.of("PATIENT"))
        );
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, List.of(() -> "ROLE_PATIENT"));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
