package com.healthcare.eventmessaging.exception;

public class MonitoringDispatchFailedException extends RuntimeException {
    public MonitoringDispatchFailedException(String messageId, Throwable cause) {
        super("Monitoring dispatch failed for message id=" + messageId, cause);
    }
}
