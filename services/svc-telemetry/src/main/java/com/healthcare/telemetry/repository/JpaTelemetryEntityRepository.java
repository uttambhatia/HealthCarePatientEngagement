package com.healthcare.telemetry.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaTelemetryEntityRepository extends JpaRepository<TelemetryReadingEntity, String> {
	List<TelemetryReadingEntity> findByDeviceIdIn(List<String> deviceIds);
}