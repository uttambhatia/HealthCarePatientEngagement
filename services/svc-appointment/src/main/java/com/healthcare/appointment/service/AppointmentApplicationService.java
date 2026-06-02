package com.healthcare.appointment.service;

import com.healthcare.appointment.dto.CreateAppointmentRequest;
import com.healthcare.appointment.dto.AppointmentResponse;
import com.healthcare.appointment.dto.AvailableSlotResponse;
import com.healthcare.appointment.dto.CompleteTeleconsultationRequest;
import com.healthcare.appointment.dto.TeleconsultationResponse;

import java.util.List;

public interface AppointmentApplicationService {
    AppointmentResponse bookAppointment(CreateAppointmentRequest request, String correlationId);
    AppointmentResponse getAppointment(String id);
    List<AppointmentResponse> listAppointments();
    AvailableSlotResponse listAvailableSlots(String providerId, String date);
    TeleconsultationResponse startTeleconsultation(String appointmentId, String correlationId);
    TeleconsultationResponse joinTeleconsultation(String appointmentId, String correlationId);
    TeleconsultationResponse completeTeleconsultation(String appointmentId, CompleteTeleconsultationRequest request, String correlationId);
}
