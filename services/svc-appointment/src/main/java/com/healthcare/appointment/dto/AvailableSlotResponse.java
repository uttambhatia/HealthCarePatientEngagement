package com.healthcare.appointment.dto;

import java.util.List;

public record AvailableSlotResponse(
        String providerId,
        String date,
        List<String> availableSlots
) {
}
