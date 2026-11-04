package com.example.usermanagement.exception;

/**
 * Base class for business exceptions that map directly to an {@link ErrorCode} and HTTP status.
 */
public abstract class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    protected ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
