package com.healthcare.identityadapter.controller;

import com.healthcare.identityadapter.service.UserProfilePhotoStorageService;
import com.healthcare.platform.common.observability.CorrelationIdHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/identity/assertions/profile-photo")
@Tag(name = "ProfilePhoto")
public class ProfilePhotoController {
    private final UserProfilePhotoStorageService storageService;

    public ProfilePhotoController(UserProfilePhotoStorageService storageService) {
        this.storageService = storageService;
    }

    @Operation(summary = "Upload profile photo for the authenticated user")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> upload(@RequestParam("file") MultipartFile file, Authentication authentication) {
        String subject = resolveSubject(authentication);
        String correlationId = CorrelationIdHolder.get().orElse("n/a");
        storageService.upload(subject, file, correlationId);
        return ResponseEntity.ok().header("X-Correlation-Id", correlationId).build();
    }

    @Operation(summary = "Get profile photo for the authenticated user")
    @GetMapping
    public ResponseEntity<byte[]> get(Authentication authentication) {
        String subject = resolveSubject(authentication);
        String correlationId = CorrelationIdHolder.get().orElse("n/a");
        UserProfilePhotoStorageService.PhotoContent photo = storageService.downloadLatest(subject, correlationId);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (photo.contentType() != null && !photo.contentType().isBlank()) {
            mediaType = MediaType.parseMediaType(photo.contentType());
        }

        return ResponseEntity.ok()
                .header("X-Correlation-Id", correlationId)
                .header("Content-Disposition", "inline; filename=\"" + photo.fileName() + "\"")
                .contentType(mediaType)
                .body(photo.content());
    }

    private String resolveSubject(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authenticated JWT subject.");
        }

        String[] candidates = {"preferred_username", "upn", "email", "sub"};
        for (String claim : candidates) {
            String value = jwtAuthenticationToken.getToken().getClaimAsString(claim);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authenticated JWT subject.");
    }
}