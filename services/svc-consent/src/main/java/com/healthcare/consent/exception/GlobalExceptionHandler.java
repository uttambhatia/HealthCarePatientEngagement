package com.healthcare.consent.exception;

import com.healthcare.platform.common.exception.ApiError;
import com.healthcare.platform.common.observability.CorrelationIdHolder;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("RESOURCE_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiError> handleValidation(Exception exception) {
        return ResponseEntity.badRequest().body(error("VALIDATION_ERROR", exception.getMessage()));
    }

    private ApiError error(String code, String message) {
        return new ApiError(CorrelationIdHolder.get().orElse("n/a"), code, message, OffsetDateTime.now());
    }
}
