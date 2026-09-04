package com.backend.backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@Getter
public class PinApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final Instant lockedUntil;
    private final Long retryAfterSeconds;

    public PinApiException(HttpStatus status, String code, String message) {
        this(status, code, message, null, null);
    }

    public PinApiException(
            HttpStatus status,
            String code,
            String message,
            Instant lockedUntil,
            Long retryAfterSeconds
    ) {
        super(message);
        this.status = status;
        this.code = code;
        this.lockedUntil = lockedUntil;
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
