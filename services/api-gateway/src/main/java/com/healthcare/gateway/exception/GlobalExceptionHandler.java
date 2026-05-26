package com.healthcare.gateway.exception;

import com.healthcare.platform.common.exception.ApiError;
import com.healthcare.platform.common.observability.CorrelationIdHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handle(Exception exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiError(CorrelationIdHolder.get().orElse("n/a"), "GATEWAY_ERROR", exception.getMessage(), OffsetDateTime.now()));
    }
}
