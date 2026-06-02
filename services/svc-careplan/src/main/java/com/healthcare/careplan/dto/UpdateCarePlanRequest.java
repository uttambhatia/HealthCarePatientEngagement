package com.healthcare.careplan.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateCarePlanRequest(
        @NotBlank String goal,
        @NotBlank String planStatus,
        @NotBlank String ownerId,
        @NotEmpty List<@NotBlank String> tasks,
        @Min(1) int expectedVersion
) {
}
