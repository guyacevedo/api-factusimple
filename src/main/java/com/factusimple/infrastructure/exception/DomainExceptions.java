package com.factusimple.infrastructure.exception;

import org.springframework.http.HttpStatus;

/** Excepciones de dominio tipadas mapeadas a estados HTTP. */
public final class DomainExceptions {

    private DomainExceptions() {
    }

    public static class BadRequestException extends AppException {
        public BadRequestException(String errorCode, String message) {
            super(HttpStatus.BAD_REQUEST, errorCode, message);
        }
    }

    public static class UnauthorizedException extends AppException {
        public UnauthorizedException(String errorCode, String message) {
            super(HttpStatus.UNAUTHORIZED, errorCode, message);
        }
    }

    public static class ForbiddenException extends AppException {
        public ForbiddenException(String errorCode, String message) {
            super(HttpStatus.FORBIDDEN, errorCode, message);
        }
    }

    public static class NotFoundException extends AppException {
        public NotFoundException(String errorCode, String message) {
            super(HttpStatus.NOT_FOUND, errorCode, message);
        }
    }

    public static class ConflictException extends AppException {
        public ConflictException(String errorCode, String message) {
            super(HttpStatus.CONFLICT, errorCode, message);
        }
    }

    public static class UnprocessableEntityException extends AppException {
        public UnprocessableEntityException(String errorCode, String message) {
            super(HttpStatus.UNPROCESSABLE_ENTITY, errorCode, message);
        }
    }

    public static class ProviderUnavailableException extends AppException {
        public ProviderUnavailableException(String errorCode, String message) {
            super(HttpStatus.SERVICE_UNAVAILABLE, errorCode, message);
        }
    }
}
