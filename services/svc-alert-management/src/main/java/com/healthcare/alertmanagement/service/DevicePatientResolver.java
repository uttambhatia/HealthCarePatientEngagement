package com.healthcare.alertmanagement.service;

import com.healthcare.alertmanagement.repository.JpaDevicePatientMappingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class DevicePatientResolver {
    private final JpaDevicePatientMappingRepository mappingRepository;
    private final Map<String, String> patientByDeviceId;

    public DevicePatientResolver(
            JpaDevicePatientMappingRepository mappingRepository,
            @Value("${platform.integration.device-patient.mappings:}") String mappingsRaw) {
        this.mappingRepository = mappingRepository;
        this.patientByDeviceId = parseMappings(mappingsRaw);
    }

    public Optional<String> resolvePatientId(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return Optional.empty();
        }
        Optional<String> dbResolved = mappingRepository.findById(deviceId).map(mapping -> mapping.getPatientId());
        if (dbResolved.isPresent()) {
            return dbResolved;
        }
        return Optional.ofNullable(patientByDeviceId.get(deviceId));
    }

    private Map<String, String> parseMappings(String mappingsRaw) {
        Map<String, String> result = new LinkedHashMap<>();
        if (mappingsRaw == null || mappingsRaw.isBlank()) {
            return result;
        }

        String[] mappings = mappingsRaw.split(",");
        for (String entry : mappings) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            int separator = entry.indexOf(':');
            if (separator <= 0 || separator >= entry.length() - 1) {
                continue;
            }
            String deviceId = entry.substring(0, separator).trim();
            String patientId = entry.substring(separator + 1).trim();
            if (!deviceId.isBlank() && !patientId.isBlank()) {
                result.put(deviceId, patientId);
            }
        }
        return result;
    }
}
