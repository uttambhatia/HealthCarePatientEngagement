package com.healthcare.appointment.exception;

public class InsecureSessionConfigurationException extends RuntimeException {
    public InsecureSessionConfigurationException(String baseUrl) {
        super("Teleconsultation base URL must use HTTPS. configuredBaseUrl=" + baseUrl);
    }
}
