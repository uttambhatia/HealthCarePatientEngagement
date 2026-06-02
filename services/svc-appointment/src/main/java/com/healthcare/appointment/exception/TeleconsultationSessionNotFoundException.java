package com.healthcare.appointment.exception;

public class TeleconsultationSessionNotFoundException extends RuntimeException {
    public TeleconsultationSessionNotFoundException(String appointmentId) {
        super("Teleconsultation session not found for appointmentId=" + appointmentId);
    }
}
