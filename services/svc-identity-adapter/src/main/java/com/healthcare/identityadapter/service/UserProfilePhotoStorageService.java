package com.healthcare.identityadapter.service;

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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Service
public class UserProfilePhotoStorageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserProfilePhotoStorageService.class);
    private static final long MAX_PHOTO_SIZE_BYTES = 5L * 1024 * 1024;

    private final BlobContainerClient containerClient;

    public UserProfilePhotoStorageService(
            @Value("${platform.integration.blob.connection-string:}") String connectionString,
            @Value("${platform.integration.blob.profile-photo-container:user-profile-photos}") String containerName) {
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

    public PhotoUploadResult upload(String subject, MultipartFile file, String correlationId) {
        if (containerClient == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Profile photo storage is not configured.");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile photo file is required.");
        }
        if (file.getSize() > MAX_PHOTO_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Profile photo exceeds the 5MB size limit.");
        }

        String fileName = sanitizeFileName(file.getOriginalFilename());
        String blobName = "profile-photo/" + sanitizePathSegment(subject) + "/" + Instant.now().toEpochMilli() + "-" + fileName;
        String contentType = (file.getContentType() == null || file.getContentType().isBlank())
                ? "application/octet-stream"
                : file.getContentType();

        try {
            byte[] bytes = file.getBytes();
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            BlobParallelUploadOptions options = new BlobParallelUploadOptions(new ByteArrayInputStream(bytes));
            options.setHeaders(new BlobHttpHeaders().setContentType(contentType));
            options.setMetadata(Map.of("originalFileName", fileName, "subject", subject));
            options.setTags(Map.of("pii", "true", "hipaa", "true", "documentType", "profile-photo"));
            blobClient.uploadWithResponse(options, null, null);
            return new PhotoUploadResult(blobName, contentType, fileName);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read profile photo content.", ex);
        } catch (BlobStorageException ex) {
            LOGGER.error("Profile photo upload failed subject={} correlationId={} error={}",
                    subject, correlationId, ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Unable to store profile photo in Azure Blob Storage.", ex);
        }
    }

    public PhotoContent downloadLatest(String subject, String correlationId) {
        if (containerClient == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Profile photo storage is not configured.");
        }

        String prefix = "profile-photo/" + sanitizePathSegment(subject) + "/";
        BlobClient latestBlob = null;
        for (var item : containerClient.listBlobsByHierarchy(prefix)) {
            if (item.isPrefix()) {
                continue;
            }
            BlobClient candidate = containerClient.getBlobClient(item.getName());
            if (latestBlob == null || candidate.getProperties().getLastModified().isAfter(latestBlob.getProperties().getLastModified())) {
                latestBlob = candidate;
            }
        }

        if (latestBlob == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No profile photo uploaded for this user.");
        }

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            latestBlob.downloadStream(output);
            String contentType = latestBlob.getProperties().getContentType();
            String fileName = latestBlob.getProperties().getMetadata().getOrDefault("originalFileName", "profile-photo");
            return new PhotoContent(output.toByteArray(), contentType, fileName);
        } catch (BlobStorageException ex) {
            LOGGER.error("Profile photo download failed subject={} correlationId={} error={}",
                    subject, correlationId, ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Unable to load profile photo from Azure Blob Storage.", ex);
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "profile-photo.bin";
        }
        return fileName.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String sanitizePathSegment(String subject) {
        if (subject == null || subject.isBlank()) {
            return "anonymous";
        }
        return subject.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    public record PhotoUploadResult(String blobName, String contentType, String fileName) {
    }

    public record PhotoContent(byte[] content, String contentType, String fileName) {
    }
}