package com.backend.backend.service;

import com.backend.backend.entity.AccountEntity;
import com.backend.backend.entity.IpadDeviceEntity;
import com.backend.backend.entity.LocationEntity;
import com.backend.backend.entity.UserAccountPinEntity;
import com.backend.backend.entity.UserEntity;
import com.backend.backend.enums.PinCredentialStatus;
import com.backend.backend.exception.PinApiException;
import com.backend.backend.repositories.AccountRepository;
import com.backend.backend.repositories.IpadDeviceRepository;
import com.backend.backend.repositories.LocationRepository;
import com.backend.backend.repositories.UserAccountAccessRepository;
import com.backend.backend.repositories.UserAccountPinRepository;
import com.backend.backend.repositories.UserLocationAccessRepository;
import com.backend.backend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserAccountPinServiceLockoutTest {
    private final UUID accountId = UUID.randomUUID();
    private final UUID locationId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID deviceId = UUID.randomUUID();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-09-01T12:00:00Z"));

    private final UserAccountPinRepository pins = mock(UserAccountPinRepository.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final LocationRepository locations = mock(LocationRepository.class);
    private final UserAccountAccessRepository accountAccess = mock(UserAccountAccessRepository.class);
    private final UserLocationAccessRepository locationAccess = mock(UserLocationAccessRepository.class);
    private final IpadDeviceRepository devices = mock(IpadDeviceRepository.class);
    private final AccountAuthorizationService authorization = mock(AccountAuthorizationService.class);
    private final PinCryptoService crypto = mock(PinCryptoService.class);
    private final PinRateLimitService rateLimit = mock(PinRateLimitService.class);
    private final PinActionTokenService tokens = mock(PinActionTokenService.class);
    private final PinAuditService audit = mock(PinAuditService.class);
    private final AuditRequestMetadataProvider metadata = mock(AuditRequestMetadataProvider.class);

    private UserAccountPinEntity credential;
    private UserAccountPinService service;

    @BeforeEach
    void setUp() {
        AccountEntity account = new AccountEntity();
        account.setId(accountId);
        account.setAccountActive(true);
        LocationEntity location = new LocationEntity();
        location.setId(locationId);
        location.setAccount(account);
        location.setLocationActive(true);
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setUserActive(true);
        IpadDeviceEntity device = new IpadDeviceEntity();
        device.setId(deviceId);
        device.setAccount(account);
        device.setLocation(location);
        device.setActive(true);
        credential = new UserAccountPinEntity();
        credential.setId(UUID.randomUUID());
        credential.setAccount(account);
        credential.setUser(user);
        credential.setStatus(PinCredentialStatus.ACTIVE);
        credential.setCredentialVersion(3);
        credential.setOnlinePinHash("encoded");

        when(accounts.findByIdForUpdate(accountId)).thenReturn(Optional.of(account));
        when(devices.findByIdForUpdate(deviceId)).thenReturn(Optional.of(device));
        when(locations.findById(locationId)).thenReturn(Optional.of(location));
        when(users.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(accountAccess.existsByUserIdAndAccountId(userId, accountId)).thenReturn(true);
        when(locationAccess.existsByUserIdAndLocationId(userId, locationId)).thenReturn(true);
        when(pins.findByAccountIdAndUserIdForUpdate(accountId, userId)).thenReturn(Optional.of(credential));
        when(metadata.current()).thenReturn(new AuditRequestMetadataProvider.AuditMetadata("203.0.113.5", "test", "correlation"));
        when(rateLimit.recordFailure(any(), anyString())).thenReturn(false);
        when(crypto.matchesOnlineHash(accountId, userId, "1234", "encoded")).thenReturn(true);
        when(tokens.issue(userId, accountId, locationId, deviceId, 3)).thenReturn("restricted-token");

        service = new UserAccountPinService(
                pins, accounts, users, locations, accountAccess, locationAccess, devices,
                authorization, crypto, new PinLockoutPolicy(), rateLimit, tokens, audit, metadata, clock
        );
    }

    @Test
    void fifthFailureLocksForFiveMinutesAndLockedRequestsDoNotIncrement() {
        for (int attempt = 1; attempt <= 4; attempt++) {
            assertThatThrownBy(() -> verify("9999"))
                    .isInstanceOf(PinApiException.class)
                    .extracting("status.value")
                    .isEqualTo(401);
        }

        assertThatThrownBy(() -> verify("9999"))
                .isInstanceOf(PinApiException.class)
                .extracting("status.value")
                .isEqualTo(423);
        assertThat(credential.getFailedAttempts()).isEqualTo(5);
        assertThat(credential.getLockedUntil()).isEqualTo(clock.instant().plus(Duration.ofMinutes(5)));

        assertThatThrownBy(() -> verify("9999")).isInstanceOf(PinApiException.class);
        assertThat(credential.getFailedAttempts()).isEqualTo(5);
    }

    @Test
    void eachPostExpirationFailureAdvancesTheLockSchedule() {
        credential.setFailedAttempts(4);
        Duration[] expected = {
                Duration.ofMinutes(5), Duration.ofMinutes(15), Duration.ofHours(1),
                Duration.ofHours(4), Duration.ofHours(8), Duration.ofHours(24), Duration.ofHours(24)
        };
        for (Duration duration : expected) {
            Instant failureTime = clock.instant();
            assertThatThrownBy(() -> verify("9999")).isInstanceOf(PinApiException.class);
            assertThat(credential.getLockedUntil()).isEqualTo(failureTime.plus(duration));
            clock.advance(duration.plusSeconds(1));
        }
        assertThat(credential.getFailedAttempts()).isEqualTo(11);
    }

    @Test
    void successfulAuthenticationResetsLockState() {
        credential.setFailedAttempts(7);
        credential.setLockoutLevel(3);
        credential.setLockedUntil(clock.instant().minusSeconds(1));
        credential.setLastFailedAt(clock.instant().minusSeconds(60));

        assertThat(verify("1234").employeeActionToken()).isEqualTo("restricted-token");
        assertThat(credential.getFailedAttempts()).isZero();
        assertThat(credential.getLockoutLevel()).isZero();
        assertThat(credential.getLockedUntil()).isNull();
        assertThat(credential.getLastFailedAt()).isNull();
    }

    @Test
    void inactiveUserAndMissingLocationAccessCannotAuthenticate() {
        credential.getUser().setUserActive(false);
        assertThatThrownBy(() -> verify("1234"))
                .isInstanceOf(PinApiException.class)
                .extracting("code")
                .isEqualTo("PIN_VERIFICATION_FAILED");

        credential.getUser().setUserActive(true);
        when(locationAccess.existsByUserIdAndLocationId(userId, locationId)).thenReturn(false);
        assertThatThrownBy(() -> verify("1234"))
                .isInstanceOf(PinApiException.class)
                .extracting("code")
                .isEqualTo("PIN_VERIFICATION_FAILED");
    }

    @Test
    void revokedPinCannotAuthenticate() {
        credential.setStatus(PinCredentialStatus.REVOKED);
        assertThatThrownBy(() -> verify("1234"))
                .isInstanceOf(PinApiException.class)
                .extracting("code")
                .isEqualTo("PIN_VERIFICATION_FAILED");
    }

    private com.backend.backend.dto.PinVerificationResponse verify(String pin) {
        return service.verifyOnline(accountId, locationId, userId, pin, deviceId);
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
