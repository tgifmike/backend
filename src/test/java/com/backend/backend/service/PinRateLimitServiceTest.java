package com.backend.backend.service;

import com.backend.backend.entity.PinAuthThrottleEntity;
import com.backend.backend.exception.PinApiException;
import com.backend.backend.repositories.PinAuthThrottleRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PinRateLimitServiceTest {
    private final MutableClock clock = new MutableClock(Instant.parse("2026-09-01T12:00:00Z"));
    private final Map<String, PinAuthThrottleEntity> state = new HashMap<>();
    private final PinRateLimitService service = new PinRateLimitService(repository(), clock);

    @Test
    void accountAndIpBlocksAfterTwentyFailuresForFifteenMinutes() {
        UUID accountId = UUID.randomUUID();
        for (int i = 0; i < 20; i++) {
            service.checkAllowed(accountId, "203.0.113.5");
            service.recordFailure(accountId, "203.0.113.5");
        }

        assertThatThrownBy(() -> service.checkAllowed(accountId, "203.0.113.5"))
                .isInstanceOf(PinApiException.class)
                .extracting("code")
                .isEqualTo("PIN_RATE_LIMITED");

        clock.advance(Duration.ofMinutes(15).plusSeconds(1));
        service.checkAllowed(accountId, "203.0.113.5");
    }

    @Test
    void entireAccountThrottlesAfterOneHundredFailuresAcrossIps() {
        UUID accountId = UUID.randomUUID();
        for (int i = 0; i < 100; i++) {
            service.recordFailure(accountId, "198.51.100." + i);
        }

        assertThatThrownBy(() -> service.checkAllowed(accountId, "192.0.2.10"))
                .isInstanceOf(PinApiException.class)
                .extracting("retryAfterSeconds")
                .isEqualTo(900L);
    }

    @SuppressWarnings("unchecked")
    private PinAuthThrottleRepository repository() {
        return (PinAuthThrottleRepository) Proxy.newProxyInstance(
                PinAuthThrottleRepository.class.getClassLoader(),
                new Class<?>[]{PinAuthThrottleRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findForUpdate")) {
                        return Optional.ofNullable(state.get(key((String) args[0], (UUID) args[1], (String) args[2])));
                    }
                    if (method.getName().equals("save")) {
                        PinAuthThrottleEntity entity = (PinAuthThrottleEntity) args[0];
                        state.put(key(entity.getScopeType(), entity.getAccountId(), entity.getIpAddress()), entity);
                        return entity;
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static String key(String scope, UUID accountId, String ip) {
        return scope + ":" + accountId + ":" + ip;
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
