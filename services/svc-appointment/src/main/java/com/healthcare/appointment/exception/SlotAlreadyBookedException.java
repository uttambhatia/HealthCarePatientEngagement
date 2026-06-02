package com.healthcare.appointment.exception;

public class SlotAlreadyBookedException extends RuntimeException {
    public SlotAlreadyBookedException(String providerId, String scheduledAt) {
        super("Slot is already booked for provider=" + providerId + " at=" + scheduledAt);
    }
}
