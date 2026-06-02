package com.healthcare.deviceingestion.exception;

public class UnregisteredDeviceException extends RuntimeException {
    public UnregisteredDeviceException(String deviceId) {
        super("Device is not registered: " + deviceId);
    }
}
