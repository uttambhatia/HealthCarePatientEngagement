package com.healthcare.platform.common.exception;

import java.time.OffsetDateTime;

public record ApiError(String correlationId, String code, String message, OffsetDateTime timestamp) {
}
