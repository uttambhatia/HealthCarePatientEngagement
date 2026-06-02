package com.healthcare.appointment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaAppointmentEntityRepository extends JpaRepository<AppointmentRecordEntity, String> {
	boolean existsByProviderIdAndScheduledAtAndStatus(String providerId, String scheduledAt, String status);
	List<AppointmentRecordEntity> findByProviderIdAndScheduledAtStartingWithAndStatus(String providerId, String datePrefix, String status);
}