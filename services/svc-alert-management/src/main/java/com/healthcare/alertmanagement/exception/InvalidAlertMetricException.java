package com.healthcare.alertmanagement.exception;

public class InvalidAlertMetricException extends RuntimeException {
    public InvalidAlertMetricException(String message) {
        super(message);
    }
}
