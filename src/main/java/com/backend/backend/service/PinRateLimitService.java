package com.backend.backend.service;

import com.backend.backend.entity.PinAuthThrottleEntity;
import com.backend.backend.exception.PinApiException;
import com.backend.backend.repositories.PinAuthThrottleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PinRateLimitService {
    private static final Duration WINDOW = Duration.ofMinutes(10);
    private static final Duration BLOCK = Duration.ofMinutes(15);
    private static final String ACCOUNT_IP = "ACCOUNT_IP";
    private static final String ACCOUNT = "ACCOUNT";
    private static final String ACCOUNT_SENTINEL = "*";

    private final PinAuthThrottleRepository repository;
    private final Clock clock;

    public void checkAllowed(UUID accountId, String ipAddress) {
        Instant now = clock.instant();
        checkScope(ACCOUNT_IP, accountId, normalizeIp(ipAddress), now);
        checkScope(ACCOUNT, accountId, ACCOUNT_SENTINEL, now);
    }

    public boolean recordFailure(UUID accountId, String ipAddress) {
        Instant now = clock.instant();
        increment(ACCOUNT_IP, accountId, normalizeIp(ipAddress), 20, now);
        return increment(ACCOUNT, accountId, ACCOUNT_SENTINEL, 100, now);
    }

    private void checkScope(String scope, UUID accountId, String ipAddress, Instant now) {
        repository.findForUpdate(scope, accountId, ipAddress).ifPresent(throttle -> {
            if (throttle.getBlockedUntil() != null && throttle.getBlockedUntil().isAfter(now)) {
                long retryAfter = Math.max(1, Duration.between(now, throttle.getBlockedUntil()).toSeconds());
                throw new PinApiException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "PIN_RATE_LIMITED",
                        "Too many failed PIN verification requests",
                        throttle.getBlockedUntil(),
                        retryAfter
                );
            }
        });
    }

    private boolean increment(String scope, UUID accountId, String ipAddress, int threshold, Instant now) {
        PinAuthThrottleEntity throttle = repository.findForUpdate(scope, accountId, ipAddress)
                .orElseGet(() -> newThrottle(scope, accountId, ipAddress, now));
        if (!throttle.getWindowStartedAt().plus(WINDOW).isAfter(now)) {
            throttle.setWindowStartedAt(now);
            throttle.setFailedCount(0);
            throttle.setBlockedUntil(null);
        }
        throttle.setFailedCount(throttle.getFailedCount() + 1);
        boolean newlyBlocked = throttle.getFailedCount() == threshold;
        if (throttle.getFailedCount() >= threshold) {
            throttle.setBlockedUntil(now.plus(BLOCK));
        }
        repository.save(throttle);
        return newlyBlocked;
    }

    private static PinAuthThrottleEntity newThrottle(String scope, UUID accountId, String ipAddress, Instant now) {
        PinAuthThrottleEntity throttle = new PinAuthThrottleEntity();
        throttle.setScopeType(scope);
        throttle.setAccountId(accountId);
        throttle.setIpAddress(ipAddress);
        throttle.setWindowStartedAt(now);
        return throttle;
    }

    private static String normalizeIp(String ipAddress) {
        return ipAddress == null || ipAddress.isBlank() ? "unknown" : ipAddress;
    }
}
