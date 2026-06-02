package com.healthcare.notification.port;

public interface AppointmentBookingPort {
    /**
     * Creates a follow-up appointment draft.
     *
     * @return true when the appointment was created, false when skipped (e.g. no base URL configured).
     */
    boolean createFollowUpAppointment(String patientId, String providerId, String scheduledAt, String correlationId);
}
