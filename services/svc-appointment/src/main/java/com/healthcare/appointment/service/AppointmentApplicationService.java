package com.healthcare.appointment.service;

import com.healthcare.appointment.dto.CreateAppointmentRequest;
import com.healthcare.appointment.dto.AppointmentResponse;

import java.util.List;

public interface AppointmentApplicationService {
    AppointmentResponse bookAppointment(CreateAppointmentRequest request, String correlationId);
    AppointmentResponse getAppointment(String id);
    List<AppointmentResponse> listAppointments();
}
