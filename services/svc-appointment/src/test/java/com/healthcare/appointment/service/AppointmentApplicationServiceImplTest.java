package com.healthcare.appointment.service;

import com.healthcare.appointment.dto.CreateAppointmentRequest;
import com.healthcare.appointment.exception.SlotAlreadyBookedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AppointmentApplicationServiceImplTest {
    @Autowired
    private AppointmentApplicationService service;

    @Test
    void shouldBookAppointmentWhenConsentEnforcementDisabled() {
        var created = service.bookAppointment(
                new CreateAppointmentRequest("pat-1001", "prov-44", "2026-06-01T09:30:00Z", "VIDEO"),
                "corr-300"
        );

        assertThat(created.id()).isNotBlank();
        assertThat(created.status()).isEqualTo("BOOKED");
    }

    @Test
    void shouldRejectBookingWhenSlotAlreadyBooked() {
        service.bookAppointment(
                new CreateAppointmentRequest("pat-2001", "prov-55", "2026-06-02T10:00:00Z", "VIDEO"),
                "corr-310"
        );

        assertThatThrownBy(() -> service.bookAppointment(
                new CreateAppointmentRequest("pat-2002", "prov-55", "2026-06-02T10:00:00Z", "VIDEO"),
                "corr-311"
        )).isInstanceOf(SlotAlreadyBookedException.class);
    }

    @Test
    void shouldListAvailableSlotsExcludingBookedOnes() {
        service.bookAppointment(
                new CreateAppointmentRequest("pat-3001", "prov-66", "2026-06-03T09:30:00Z", "VIDEO"),
                "corr-320"
        );

        var available = service.listAvailableSlots("prov-66", "2026-06-03");

        assertThat(available.providerId()).isEqualTo("prov-66");
        assertThat(available.date()).isEqualTo("2026-06-03");
        assertThat(available.availableSlots()).doesNotContain("2026-06-03T09:30:00Z");
        assertThat(available.availableSlots()).contains("2026-06-03T09:00:00Z", "2026-06-03T10:00:00Z");
        assertThat(available.availableSlots()).hasSize(15);
    }
}
