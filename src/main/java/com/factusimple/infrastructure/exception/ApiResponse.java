package com.factusimple.infrastructure.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * Envoltorio de respuesta inmutable y consistente para toda la API.
 * En éxito: {@code data} + {@code message}. En error: {@code errorCode} + {@code message}.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ApiResponse<T>(
        T data,
        String message,
        String errorCode,
        Instant timestamp
) {

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(data, message, null, Instant.now());
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(String message, String errorCode, T data) {
        return new ApiResponse<>(data, message, errorCode, Instant.now());
    }
}
