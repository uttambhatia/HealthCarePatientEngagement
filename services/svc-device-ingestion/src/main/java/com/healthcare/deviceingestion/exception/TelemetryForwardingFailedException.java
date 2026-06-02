package com.healthcare.deviceingestion.exception;

public class TelemetryForwardingFailedException extends RuntimeException {
    public TelemetryForwardingFailedException(String aggregateId, Throwable cause) {
        super("Telemetry forwarding failed for device ingestion id=" + aggregateId, cause);
    }
}
