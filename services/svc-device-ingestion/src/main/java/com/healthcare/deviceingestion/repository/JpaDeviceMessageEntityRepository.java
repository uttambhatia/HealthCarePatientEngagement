package com.healthcare.deviceingestion.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaDeviceMessageEntityRepository extends JpaRepository<DeviceMessageEntity, String> {
}
