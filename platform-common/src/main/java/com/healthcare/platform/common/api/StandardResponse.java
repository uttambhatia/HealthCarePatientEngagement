package com.healthcare.platform.common.api;

public record StandardResponse<T>(String correlationId, T data) {
}
