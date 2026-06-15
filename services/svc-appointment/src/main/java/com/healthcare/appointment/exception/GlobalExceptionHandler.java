package com.healthcare.appointment.exception;

import com.healthcare.platform.common.exception.ApiError;
import com.healthcare.platform.common.observability.CorrelationIdHolder;
import jakarta.validation.ConstraintViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(InsecureSessionConfigurationException.class)
    public ResponseEntity<ApiError> handleInsecureSessionConfig(InsecureSessionConfigurationException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error("INSECURE_SESSION_CONFIGURATION", exception.getMessage()));
    }

    @ExceptionHandler(ConsentAccessDeniedException.class)
    public ResponseEntity<ApiError> handleConsentDenied(ConsentAccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("CONSENT_ACCESS_DENIED", exception.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("ACCESS_DENIED", exception.getMessage()));
    }

    @ExceptionHandler(AppointmentNotEligibleException.class)
    public ResponseEntity<ApiError> handleAppointmentNotEligible(AppointmentNotEligibleException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error("APPOINTMENT_NOT_ELIGIBLE", exception.getMessage()));
    }

    @ExceptionHandler(TeleconsultationSessionNotFoundException.class)
    public ResponseEntity<ApiError> handleTeleconsultNotFound(TeleconsultationSessionNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("TELECONSULTATION_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(TeleconsultationNetworkException.class)
    public ResponseEntity<ApiError> handleTeleconsultNetwork(TeleconsultationNetworkException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error("TELECONSULTATION_NETWORK_FAILURE", exception.getMessage()));
    }

    @ExceptionHandler(SlotAlreadyBookedException.class)
    public ResponseEntity<ApiError> handleSlotConflict(SlotAlreadyBookedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error("SLOT_ALREADY_BOOKED", exception.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("RESOURCE_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiError> handleValidation(Exception exception) {
        return ResponseEntity.badRequest().body(error("VALIDATION_ERROR", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(error("VALIDATION_ERROR", exception.getMessage()));
    }

    private ApiError error(String code, String message) {
        return new ApiError(CorrelationIdHolder.get().orElse("n/a"), code, message, OffsetDateTime.now());
    }
}
