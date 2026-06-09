package com.factusimple.infrastructure.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción base de dominio. Lleva el estado HTTP y un código de error de
 * negocio estable para que el cliente pueda reaccionar programáticamente.
 */
public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public AppException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
