package com.healthcare.alertmanagement.exception;

public class AlertEscalationFailedException extends RuntimeException {
    public AlertEscalationFailedException(String alertId, Throwable cause) {
        super("Alert escalation failed for alertId=" + alertId, cause);
    }
}
