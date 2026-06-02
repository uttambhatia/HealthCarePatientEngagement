package com.healthcare.deviceingestion.exception;

public class InvalidTelemetryPayloadException extends RuntimeException {
    public InvalidTelemetryPayloadException(String message) {
        super(message);
    }
}
