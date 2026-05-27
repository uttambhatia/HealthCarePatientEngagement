package com.healthcare.deviceingestion.repository;

import com.healthcare.deviceingestion.domain.DeviceMessage;

import java.util.List;
import java.util.Optional;

public interface DeviceIngestionRepository {
    DeviceMessage save(DeviceMessage aggregate);
    Optional<DeviceMessage> findById(String id);
    List<DeviceMessage> findAll();
    void deleteById(String id);
}
