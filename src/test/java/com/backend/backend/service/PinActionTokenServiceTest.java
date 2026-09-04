package com.backend.backend.service;

import com.auth0.jwt.exceptions.TokenExpiredException;
import com.backend.backend.config.PinActionTokenFilter;
import com.backend.backend.security.PinActionPrincipal;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

class PinActionTokenServiceTest {
    private static final String SECRET = "pin-action-secret-with-at-least-32-characters";

    @Test
    void tokenContainsRestrictedScopeAndExpiresAfterTenMinutes() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-01T12:00:00Z"));
        PinActionTokenService service = new PinActionTokenService(SECRET, "pin-issuer", "line-check-api", clock);
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();

        String token = service.issue(userId, accountId, locationId, deviceId, 7);
        PinActionPrincipal principal = service.verify(token);
        assertThat(principal).isEqualTo(new PinActionPrincipal(userId, accountId, locationId, deviceId, 7));

        clock.advance(Duration.ofMinutes(9).plusSeconds(59));
        assertThat(service.verify(token).userId()).isEqualTo(userId);
        clock.advance(Duration.ofSeconds(2));
        assertThatThrownBy(() -> service.verify(token)).isInstanceOf(TokenExpiredException.class);
    }

    @Test
    void restrictedTokenCannotAuthenticateAManagerEndpoint() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-01T12:00:00Z"));
        PinActionTokenService service = new PinActionTokenService(SECRET, "pin-issuer", "line-check-api", clock);
        String token = service.issue(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1
        );
        PinActionAuthorizationService authorization = mock(PinActionAuthorizationService.class);
        doNothing().when(authorization).validateToken(any());
        PinActionTokenFilter filter = new PinActionTokenFilter(service, authorization);
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/accounts/abc/users/def/pin");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean continued = new AtomicBoolean();
        FilterChain chain = (req, res) -> continued.set(true);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(continued).isFalse();
    }

    @Test
    void accountScopedTokenCanLoadItsAccountLocations() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-01T12:00:00Z"));
        PinActionTokenService service = new PinActionTokenService(SECRET, "pin-issuer", "line-check-api", clock);
        UUID accountId = UUID.randomUUID();
        String token = service.issue(UUID.randomUUID(), accountId, null, UUID.randomUUID(), 1);
        PinActionAuthorizationService authorization = mock(PinActionAuthorizationService.class);
        doNothing().when(authorization).validateToken(any());
        PinActionTokenFilter filter = new PinActionTokenFilter(service, authorization);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/locations/accounts/" + accountId + "/locations"
        );
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean continued = new AtomicBoolean();
        FilterChain chain = (req, res) -> continued.set(true);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(continued).isTrue();
    }

    @Test
    void accountUnlockTokenAllowsLocationToBeSelectedLater() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-01T12:00:00Z"));
        PinActionTokenService service = new PinActionTokenService(SECRET, "pin-issuer", "line-check-api", clock);

        PinActionPrincipal principal = service.verify(service.issue(
                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(), 1
        ));

        assertThat(principal.locationId()).isNull();
    }

    @Test
    void retainedEmployeeTokenDoesNotBlockStartingAnotherPinVerification() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-01T12:00:00Z"));
        PinActionTokenService service = new PinActionTokenService(SECRET, "pin-issuer", "line-check-api", clock);
        String token = service.issue(UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(), 1);
        PinActionAuthorizationService authorization = mock(PinActionAuthorizationService.class);
        PinActionTokenFilter filter = new PinActionTokenFilter(service, authorization);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/pin/verify");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean continued = new AtomicBoolean();

        filter.doFilter(request, response, (req, res) -> continued.set(true));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(continued).isTrue();
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
