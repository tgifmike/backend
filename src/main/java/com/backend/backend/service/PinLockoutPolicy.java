package com.backend.backend.service;

import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class PinLockoutPolicy {
    public Duration durationForFailure(int failedAttempts) {
        return switch (failedAttempts) {
            case 1, 2, 3, 4 -> Duration.ZERO;
            case 5 -> Duration.ofMinutes(5);
            case 6 -> Duration.ofMinutes(15);
            case 7 -> Duration.ofHours(1);
            case 8 -> Duration.ofHours(4);
            case 9 -> Duration.ofHours(8);
            default -> Duration.ofHours(24);
        };
    }

    public int levelForFailure(int failedAttempts) {
        return Math.max(0, Math.min(6, failedAttempts - 4));
    }
}
