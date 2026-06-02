package com.healthcare.appointment.repository;

import com.healthcare.appointment.domain.TeleconsultationSession;

import java.util.Optional;

public interface TeleconsultationSessionRepository {
    TeleconsultationSession save(TeleconsultationSession session);
    Optional<TeleconsultationSession> findById(String id);
    Optional<TeleconsultationSession> findByAppointmentId(String appointmentId);
}
