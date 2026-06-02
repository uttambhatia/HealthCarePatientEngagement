package com.healthcare.notification.exception;

public class NotificationDeliveryFailedException extends RuntimeException {
    public NotificationDeliveryFailedException(String notificationId, String reason, Throwable cause) {
        super("Notification delivery failed for id=" + notificationId + " reason=" + reason, cause);
    }
}
