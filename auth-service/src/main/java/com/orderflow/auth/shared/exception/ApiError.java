package com.orderflow.auth.shared.exception;

public record ApiError(String message, Long retryAfterSeconds) {

    public ApiError(String message) {
        this(message, null);
    }
}
