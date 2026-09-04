package com.backend.backend.service;

import com.backend.backend.dto.PinManagementResponse;
import com.backend.backend.entity.AccountEntity;
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
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAccountPinManagementServiceTest {
    private final UUID accountId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();
    private final AccountEntity account = new AccountEntity();
    private final UserEntity user = new UserEntity();

    private final UserAccountPinRepository pins = mock(UserAccountPinRepository.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final UserAccountAccessRepository accountAccess = mock(UserAccountAccessRepository.class);
    private final AccountAuthorizationService authorization = mock(AccountAuthorizationService.class);
    private final PinCryptoService crypto = mock(PinCryptoService.class);
    private final PinAuditService audit = mock(PinAuditService.class);
    private UserAccountPinService service;

    @BeforeEach
    void setUp() {
        account.setId(accountId);
        account.setAccountActive(true);
        user.setId(userId);
        user.setUserActive(true);
        when(accounts.findByIdForUpdate(accountId)).thenReturn(Optional.of(account));
        when(users.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(accountAccess.existsByUserIdAndAccountId(userId, accountId)).thenReturn(true);
        when(crypto.createEncryptedOfflineVerifier(any()))
                .thenReturn(new PinCryptoService.EncryptedVerifier("ciphertext", "nonce", 1));
        when(crypto.createOnlineHash(any(), any(), any())).thenReturn("online-hash");

        service = new UserAccountPinService(
                pins, accounts, users, mock(LocationRepository.class), accountAccess,
                mock(UserLocationAccessRepository.class), mock(IpadDeviceRepository.class), authorization,
                crypto, new PinLockoutPolicy(), mock(PinRateLimitService.class),
                mock(PinActionTokenService.class), audit, mock(AuditRequestMetadataProvider.class),
                Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void samePinIsRejectedWhenOwnedByAnotherUserInTheAccount() {
        UUID otherUserId = UUID.randomUUID();
        UserEntity otherUser = new UserEntity();
        otherUser.setId(otherUserId);
        UserAccountPinEntity existing = new UserAccountPinEntity();
        existing.setUser(otherUser);
        when(crypto.lookupDigest(accountId, "048291")).thenReturn("digest");
        when(pins.findByAccountIdAndPinLookupDigest(accountId, "digest")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.setManualPin(accountId, userId, "048291", actorId))
                .isInstanceOf(PinApiException.class)
                .extracting("code")
                .isEqualTo("PIN_ALREADY_IN_USE");
    }

    @Test
    void generationRetriesCollisionUnderAccountDatabaseLockAndReturnsPinOnce() {
        when(crypto.generatePin(6)).thenReturn("000000", "048291");
        when(crypto.lookupDigest(accountId, "000000")).thenReturn("collision");
        when(crypto.lookupDigest(accountId, "048291")).thenReturn("available");
        when(pins.existsByAccountIdAndPinLookupDigest(accountId, "collision")).thenReturn(true);
        when(pins.existsByAccountIdAndPinLookupDigest(accountId, "available")).thenReturn(false);
        when(pins.findByAccountIdAndUserIdForUpdate(accountId, userId)).thenReturn(Optional.empty());

        PinManagementResponse response = service.generatePin(accountId, userId, 6, actorId);

        assertThat(response.pin()).isEqualTo("048291");
        assertThat(response.credentialVersion()).isEqualTo(1);
        verify(accounts).findByIdForUpdate(accountId);
        ArgumentCaptor<UserAccountPinEntity> saved = ArgumentCaptor.forClass(UserAccountPinEntity.class);
        verify(pins).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getPinLookupDigest()).isEqualTo("available");
        assertThat(saved.getValue().getOnlinePinHash()).isEqualTo("online-hash");
        assertThat(saved.getValue().getEncryptedOfflineVerifier()).isEqualTo("ciphertext");
        assertThat(saved.getValue().toString()).doesNotContain("048291");
    }

    @Test
    void replacementRevocationAndUnlockResetLockoutState() {
        UserAccountPinEntity credential = credential();
        when(crypto.lookupDigest(accountId, "048291")).thenReturn("digest");
        when(pins.findByAccountIdAndPinLookupDigest(accountId, "digest")).thenReturn(Optional.empty());
        when(pins.findByAccountIdAndUserIdForUpdate(accountId, userId)).thenReturn(Optional.of(credential));

        service.setManualPin(accountId, userId, "048291", actorId);
        assertThat(credential.getCredentialVersion()).isEqualTo(5);
        assertLockoutReset(credential);

        credential.setFailedAttempts(6);
        credential.setLockoutLevel(2);
        credential.setLockedUntil(Instant.now().plusSeconds(60));
        service.unlockPin(accountId, userId, actorId);
        assertThat(credential.getCredentialVersion()).isEqualTo(5);
        assertLockoutReset(credential);

        credential.setFailedAttempts(5);
        service.revokePin(accountId, userId, actorId);
        assertThat(credential.getStatus()).isEqualTo(PinCredentialStatus.REVOKED);
        assertThat(credential.getCredentialVersion()).isEqualTo(6);
        assertThat(credential.getPinLookupDigest()).isNull();
        assertThat(credential.getOnlinePinHash()).isNull();
        assertThat(credential.getEncryptedOfflineVerifier()).isNull();
        assertLockoutReset(credential);
    }

    private UserAccountPinEntity credential() {
        UserAccountPinEntity credential = new UserAccountPinEntity();
        credential.setId(UUID.randomUUID());
        credential.setAccount(account);
        credential.setUser(user);
        credential.setStatus(PinCredentialStatus.ACTIVE);
        credential.setCredentialVersion(4);
        credential.setFailedAttempts(5);
        credential.setLockoutLevel(1);
        credential.setLockedUntil(Instant.now().plusSeconds(60));
        credential.setLastFailedAt(Instant.now());
        return credential;
    }

    private static void assertLockoutReset(UserAccountPinEntity credential) {
        assertThat(credential.getFailedAttempts()).isZero();
        assertThat(credential.getLockoutLevel()).isZero();
        assertThat(credential.getLockedUntil()).isNull();
        assertThat(credential.getLastFailedAt()).isNull();
    }
}
