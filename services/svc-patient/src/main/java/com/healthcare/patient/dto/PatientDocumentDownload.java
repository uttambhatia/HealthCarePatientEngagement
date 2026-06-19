package com.healthcare.patient.dto;

public record PatientDocumentDownload(
        byte[] content,
        String contentType,
        String fileName
) {
}