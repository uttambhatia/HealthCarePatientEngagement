package com.healthcare.appointment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaTeleconsultationSessionEntityRepository extends JpaRepository<TeleconsultationSessionEntity, String> {
    Optional<TeleconsultationSessionEntity> findByAppointmentId(String appointmentId);
}
