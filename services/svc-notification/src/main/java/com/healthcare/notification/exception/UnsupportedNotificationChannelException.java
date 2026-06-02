package com.healthcare.notification.exception;

public class UnsupportedNotificationChannelException extends RuntimeException {
    public UnsupportedNotificationChannelException(String channel) {
        super("Unsupported notification channel: " + channel);
    }
}
