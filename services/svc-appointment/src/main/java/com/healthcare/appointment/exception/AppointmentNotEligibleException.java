package com.healthcare.appointment.exception;

public class AppointmentNotEligibleException extends RuntimeException {
    public AppointmentNotEligibleException(String appointmentId, String reason) {
        super("Appointment is not eligible for teleconsultation appointmentId=" + appointmentId + " reason=" + reason);
    }
}
