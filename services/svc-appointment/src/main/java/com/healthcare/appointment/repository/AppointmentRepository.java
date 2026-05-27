package com.healthcare.appointment.repository;

import com.healthcare.appointment.domain.AppointmentRecord;

import java.util.List;
import java.util.Optional;

public interface AppointmentRepository {
    AppointmentRecord save(AppointmentRecord aggregate);
    Optional<AppointmentRecord> findById(String id);
    List<AppointmentRecord> findAll();
    void deleteById(String id);
}
