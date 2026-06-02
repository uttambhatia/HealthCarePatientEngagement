package com.healthcare.careplan.exception;

public class ProtocolValidationException extends RuntimeException {
    public ProtocolValidationException(String message) {
        super(message);
    }
}
