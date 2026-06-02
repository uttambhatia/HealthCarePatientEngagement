package com.healthcare.appointment.exception;

public class ConsentAccessDeniedException extends RuntimeException {
    public ConsentAccessDeniedException(String message) {
        super(message);
    }
}
