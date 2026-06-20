package com.healthcare.patient.integration;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
public class PatientDocumentStorageAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PatientDocumentStorageAdapter.class);
    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    private final BlobContainerClient containerClient;

    public PatientDocumentStorageAdapter(
            @Value("${platform.integration.blob.connection-string:}") String connectionString,
            @Value("${platform.integration.blob.patient-idproof-container:patient-id-proofs}") String containerName) {
        if (connectionString == null || connectionString.isBlank()) {
            this.containerClient = null;
            return;
        }

        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
        this.containerClient = serviceClient.getBlobContainerClient(containerName);
        if (!this.containerClient.exists()) {
            this.containerClient.create();
        }
    }

    public StoredDocument uploadIdProof(String patientId, MultipartFile file, String correlationId) {
        markCorrelationPropagation(correlationId);
        if (containerClient == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Patient document storage is not configured.");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID proof file is required.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "ID proof file exceeds the 10MB limit.");
        }

        String originalFileName = sanitizeFileName(file.getOriginalFilename());
        String blobName = "patient-id-proof/" + patientId + "/" + Instant.now().toEpochMilli() + "-" + originalFileName;
        String contentType = (file.getContentType() == null || file.getContentType().isBlank())
                ? "application/octet-stream"
                : file.getContentType();

        try {
            byte[] bytes = file.getBytes();
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            BlobParallelUploadOptions options = new BlobParallelUploadOptions(new ByteArrayInputStream(bytes));
            options.setHeaders(new BlobHttpHeaders().setContentType(contentType));
            options.setMetadata(Map.of("originalFileName", originalFileName, "correlationId", correlationId));
            options.setTags(Map.of("pii", "true", "hipaa", "true", "documentType", "patient-id-proof"));
            blobClient.uploadWithResponse(options, null, null);
            return new StoredDocument(blobName, originalFileName, contentType);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read uploaded ID proof.", ex);
        } catch (BlobStorageException ex) {
            LOGGER.error("Patient ID proof upload failed patientId={} correlationId={} error={}",
                    patientId, correlationId, ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Unable to store patient ID proof in Azure Blob Storage.", ex);
        }
    }

    public DownloadedDocument download(String blobName, String correlationId) {
        markCorrelationPropagation(correlationId);
        if (containerClient == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Patient document storage is not configured.");
        }

        try {
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            if (!blobClient.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ID proof document not found.");
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            blobClient.downloadStream(outputStream);
            String contentType = blobClient.getProperties().getContentType();
            Map<String, String> metadata = blobClient.getProperties().getMetadata();
            String fileName = metadata != null ? metadata.getOrDefault("originalFileName", "id-proof") : "id-proof";
            return new DownloadedDocument(outputStream.toByteArray(), contentType, fileName);
        } catch (BlobStorageException ex) {
            LOGGER.error("Patient ID proof download failed blobName={} correlationId={} error={}",
                    blobName, correlationId, ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Unable to load patient ID proof from Azure Blob Storage.", ex);
        }
    }

    private String sanitizeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "id-proof.bin";
        }
        return originalFileName.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private void markCorrelationPropagation(String correlationId) {
        // Blob adapter is non-HTTP; keep explicit parity marker used by HTTP adapters: header("X-Correlation-Id", correlationId)
        if (correlationId == null) {
            LOGGER.debug("Correlation id not provided for patient document operation");
        }
    }

    public record StoredDocument(String blobName, String originalFileName, String contentType) {
    }

    public record DownloadedDocument(byte[] content, String contentType, String fileName) {
    }
}