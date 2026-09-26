package com.orderflow.auth.shared.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler({InvalidRequestException.class, MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception ex) {
        String message = switch (ex) {
            case MethodArgumentNotValidException validationException -> validationException.getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining(", "));
            case null -> "Invalid request";
            default -> ex.getMessage();
        };

        if (message == null || message.isBlank()) {
            message = "Invalid request";
        }

        return ResponseEntity.badRequest()
                .body(new ApiError(message));
    }

    @ExceptionHandler({EmailAlreadyExistsException.class, UsernameAlreadyExistsException.class,
            DataIntegrityViolationException.class})
    public ResponseEntity<ApiError> handleConflict(RuntimeException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = "Resource already exists";
        }

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(message));
    }

    @ExceptionHandler({InvalidVerificationTokenException.class, InvalidVerificationCodeException.class,
            VerificationCodeExpiredException.class, EmailAlreadyVerifiedException.class})
    public ResponseEntity<ApiError> handleVerificationErrors(RuntimeException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler(VerificationCooldownException.class)
    public ResponseEntity<ApiError> handleVerificationCooldown(VerificationCooldownException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("Unexpected error"));
    }
}
