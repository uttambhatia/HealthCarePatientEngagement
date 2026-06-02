package com.healthcare.alertmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaDevicePatientMappingRepository extends JpaRepository<DevicePatientMappingEntity, String> {
}
