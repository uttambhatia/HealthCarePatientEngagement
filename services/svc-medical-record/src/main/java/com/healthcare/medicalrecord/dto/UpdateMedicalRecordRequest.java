package com.healthcare.medicalrecord.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateMedicalRecordRequest(
        @NotBlank String fhirResourceType,
        @NotBlank String resourceReference,
        @NotBlank String summary,
        @NotNull @Min(1) Integer expectedVersion
) {
}
