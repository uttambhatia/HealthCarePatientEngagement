package com.healthcare.appointment.exception;

public class TeleconsultationNetworkException extends RuntimeException {
    public TeleconsultationNetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
