package com.backend.backend.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "INVALID_REQUEST",
                "message", "One or more request fields are invalid"
        ));
    }

    @ExceptionHandler(PinApiException.class)
    public ResponseEntity<Map<String, Object>> handlePinApiException(PinApiException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", exception.getCode());
        body.put("message", exception.getMessage());
        if (exception.getLockedUntil() != null) {
            body.put("lockedUntil", exception.getLockedUntil());
        }
        if (exception.getRetryAfterSeconds() != null) {
            body.put("retryAfterSeconds", exception.getRetryAfterSeconds());
        }
        ResponseEntity.BodyBuilder response = ResponseEntity.status(exception.getStatus());
        if (exception.getRetryAfterSeconds() != null) {
            response.header(HttpHeaders.RETRY_AFTER, exception.getRetryAfterSeconds().toString());
        }
        return response.body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handlePinConstraint(DataIntegrityViolationException exception) {
        String details = exception.getMostSpecificCause().getMessage();
        if (details != null && details.contains("uk_user_account_pins_account_digest")) {
            return ResponseEntity.status(409).body(Map.of(
                    "error", "PIN_ALREADY_IN_USE",
                    "message", "That PIN is already assigned within this account"
            ));
        }
        return ResponseEntity.status(409).body(Map.of(
                "error", "DATA_INTEGRITY_VIOLATION",
                "message", "The requested change conflicts with existing data"
        ));
    }
}
