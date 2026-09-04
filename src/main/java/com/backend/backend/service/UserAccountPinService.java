package com.backend.backend.service;

import com.backend.backend.dto.AccountUserDto;
import com.backend.backend.dto.OfflinePinVerifierBundleDto;
import com.backend.backend.dto.OfflinePinVerifierUserDto;
import com.backend.backend.dto.PinManagementResponse;
import com.backend.backend.dto.PinStatusDto;
import com.backend.backend.dto.PinUnlockResponse;
import com.backend.backend.dto.PinVerificationResponse;
import com.backend.backend.entity.AccountEntity;
import com.backend.backend.entity.IpadDeviceEntity;
import com.backend.backend.entity.LocationEntity;
import com.backend.backend.entity.UserAccountPinEntity;
import com.backend.backend.entity.UserEntity;
import com.backend.backend.enums.AuditActorType;
import com.backend.backend.enums.PinAuditEventType;
import com.backend.backend.enums.PinCredentialStatus;
import com.backend.backend.exception.PinApiException;
import com.backend.backend.repositories.AccountRepository;
import com.backend.backend.repositories.IpadDeviceRepository;
import com.backend.backend.repositories.LocationRepository;
import com.backend.backend.repositories.UserAccountAccessRepository;
import com.backend.backend.repositories.UserAccountPinRepository;
import com.backend.backend.repositories.UserLocationAccessRepository;
import com.backend.backend.repositories.UserRepository;
import com.backend.backend.security.DeviceAuthenticationPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAccountPinService {
    private static final int MAX_GENERATION_ATTEMPTS = 100;

    private final UserAccountPinRepository pinRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final UserAccountAccessRepository accountAccessRepository;
    private final UserLocationAccessRepository locationAccessRepository;
    private final IpadDeviceRepository deviceRepository;
    private final AccountAuthorizationService authorizationService;
    private final PinCryptoService cryptoService;
    private final PinLockoutPolicy lockoutPolicy;
    private final PinRateLimitService rateLimitService;
    private final PinActionTokenService actionTokenService;
    private final PinAuditService auditService;
    private final AuditRequestMetadataProvider metadataProvider;
    private final Clock clock;

    @Value("${pin.offline-bundle-hours:24}")
    private long offlineBundleHours;

    @Transactional
    public PinManagementResponse setManualPin(UUID accountId, UUID userId, String pin, UUID actorId) {
        authorizationService.requireCanManagePins(actorId, accountId);
        cryptoService.validatePin(pin);
        AccountEntity account = requireActiveAccountForUpdate(accountId);
        UserEntity user = requireEligibleAccountUser(accountId, userId);
        String digest = cryptoService.lookupDigest(accountId, pin);
        rejectDigestOwnedByAnotherUser(accountId, userId, digest);

        UserAccountPinEntity credential = pinRepository.findByAccountIdAndUserIdForUpdate(accountId, userId)
                .orElseGet(() -> newCredential(account, user, actorId));
        boolean replacing = credential.getId() != null;
        applyPin(credential, pin, digest, actorId, replacing);
        pinRepository.saveAndFlush(credential);

        auditService.record(PinAuditService.Event.builder()
                .eventType(replacing ? PinAuditEventType.PIN_REPLACED : PinAuditEventType.PIN_CREATED)
                .accountId(accountId)
                .targetUserId(userId)
                .actorUserId(actorId)
                .actorType(AuditActorType.USER)
                .credentialVersion(credential.getCredentialVersion())
                .build());
        return PinManagementResponse.configured(credential.getCredentialVersion());
    }

    @Transactional
    public PinManagementResponse generatePin(UUID accountId, UUID userId, int length, UUID actorId) {
        authorizationService.requireCanManagePins(actorId, accountId);
        cryptoService.validateLength(length);
        AccountEntity account = requireActiveAccountForUpdate(accountId);
        UserEntity user = requireEligibleAccountUser(accountId, userId);

        String generatedPin = null;
        String digest = null;
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String candidate = cryptoService.generatePin(length);
            String candidateDigest = cryptoService.lookupDigest(accountId, candidate);
            if (!pinRepository.existsByAccountIdAndPinLookupDigest(accountId, candidateDigest)) {
                generatedPin = candidate;
                digest = candidateDigest;
                break;
            }
        }
        if (generatedPin == null) {
            throw new PinApiException(HttpStatus.CONFLICT, "PIN_GENERATION_EXHAUSTED", "Unable to allocate a unique PIN");
        }

        UserAccountPinEntity credential = pinRepository.findByAccountIdAndUserIdForUpdate(accountId, userId)
                .orElseGet(() -> newCredential(account, user, actorId));
        boolean replacing = credential.getId() != null;
        applyPin(credential, generatedPin, digest, actorId, replacing);
        pinRepository.saveAndFlush(credential);

        auditService.record(PinAuditService.Event.builder()
                .eventType(PinAuditEventType.PIN_GENERATED)
                .accountId(accountId)
                .targetUserId(userId)
                .actorUserId(actorId)
                .actorType(AuditActorType.USER)
                .failureCategory(replacing ? "REPLACEMENT" : null)
                .credentialVersion(credential.getCredentialVersion())
                .build());
        return PinManagementResponse.generated(credential.getCredentialVersion(), generatedPin);
    }

    @Transactional
    public void revokePin(UUID accountId, UUID userId, UUID actorId) {
        authorizationService.requireCanManagePins(actorId, accountId);
        requireActiveAccountForUpdate(accountId);
        requireEligibleAccountUser(accountId, userId);
        UserAccountPinEntity credential = pinRepository.findByAccountIdAndUserIdForUpdate(accountId, userId)
                .orElseThrow(() -> new PinApiException(HttpStatus.NOT_FOUND, "PIN_NOT_CONFIGURED", "PIN is not configured"));
        credential.setStatus(PinCredentialStatus.REVOKED);
        credential.setPinLookupDigest(null);
        credential.setOnlinePinHash(null);
        credential.setEncryptedOfflineVerifier(null);
        credential.setOfflineVerifierNonce(null);
        credential.setEncryptionKeyVersion(null);
        credential.setPinLength(null);
        credential.setCredentialVersion(credential.getCredentialVersion() + 1);
        credential.setUpdatedBy(actorId);
        resetLockout(credential);
        pinRepository.save(credential);
        auditService.record(PinAuditService.Event.builder()
                .eventType(PinAuditEventType.PIN_REVOKED)
                .accountId(accountId)
                .targetUserId(userId)
                .actorUserId(actorId)
                .actorType(AuditActorType.USER)
                .credentialVersion(credential.getCredentialVersion())
                .build());
    }

    @Transactional
    public PinUnlockResponse unlockPin(UUID accountId, UUID userId, UUID actorId) {
        authorizationService.requireCanManagePins(actorId, accountId);
        requireActiveAccountForUpdate(accountId);
        requireEligibleAccountUser(accountId, userId);
        UserAccountPinEntity credential = pinRepository.findByAccountIdAndUserIdForUpdate(accountId, userId)
                .orElseThrow(() -> new PinApiException(HttpStatus.NOT_FOUND, "PIN_NOT_CONFIGURED", "PIN is not configured"));
        if (credential.getStatus() != PinCredentialStatus.ACTIVE) {
            throw new PinApiException(HttpStatus.CONFLICT, "PIN_NOT_CONFIGURED", "PIN is not configured");
        }
        resetLockout(credential);
        credential.setUpdatedBy(actorId);
        pinRepository.save(credential);
        auditService.record(PinAuditService.Event.builder()
                .eventType(PinAuditEventType.PIN_UNLOCKED_BY_MANAGER)
                .accountId(accountId)
                .targetUserId(userId)
                .actorUserId(actorId)
                .actorType(AuditActorType.USER)
                .credentialVersion(credential.getCredentialVersion())
                .build());
        return new PinUnlockResponse(credential.getStatus() == PinCredentialStatus.ACTIVE, false);
    }

    @Transactional(noRollbackFor = PinApiException.class)
    public PinVerificationResponse verifyOnline(
            UUID accountId,
            UUID locationId,
            UUID userId,
            String pin,
            UUID deviceId
    ) {
        String ipAddress = metadataProvider.current().ipAddress();
        AccountEntity account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new PinApiException(
                        HttpStatus.UNAUTHORIZED,
                        "PIN_VERIFICATION_FAILED",
                        "PIN verification failed"
                ));
        rateLimitService.checkAllowed(accountId, ipAddress);
        if (!Boolean.TRUE.equals(account.getAccountActive()) || account.getDeletedAt() != null) {
            throw verificationFailure(accountId, locationId, userId, deviceId, "ACCOUNT_INACTIVE", ipAddress);
        }

        IpadDeviceEntity device = deviceRepository.findByIdForUpdate(deviceId)
                .orElseThrow(() -> verificationFailure(accountId, locationId, userId, deviceId, "DEVICE_INVALID", ipAddress));
        if (!device.isActive()
                || !device.getAccount().getId().equals(accountId)
                || (device.getLocation() != null && locationId != null
                    && !device.getLocation().getId().equals(locationId))) {
            throw verificationFailure(accountId, locationId, userId, deviceId, "DEVICE_INVALID", ipAddress);
        }

        if (locationId != null) {
            LocationEntity location = locationRepository.findById(locationId)
                    .orElseThrow(() -> verificationFailure(accountId, locationId, userId, deviceId, "LOCATION_INVALID", ipAddress));
            if (!location.getAccount().getId().equals(accountId)
                    || !Boolean.TRUE.equals(location.getLocationActive())
                    || location.getDeletedAt() != null) {
                throw verificationFailure(accountId, locationId, userId, deviceId, "LOCATION_INVALID", ipAddress);
            }
        }

        UserEntity user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> verificationFailure(accountId, locationId, userId, deviceId, "USER_INVALID", ipAddress));
        if (!user.isUserActive()) {
            throw verificationFailure(accountId, locationId, userId, deviceId, "USER_INACTIVE", ipAddress);
        }
        if (!accountAccessRepository.existsByUserIdAndAccountId(userId, accountId)) {
            throw verificationFailure(accountId, locationId, userId, deviceId, "ACCOUNT_ACCESS_MISSING", ipAddress);
        }
        if (locationId != null && !locationAccessRepository.existsByUserIdAndLocationId(userId, locationId)) {
            throw verificationFailure(accountId, locationId, userId, deviceId, "LOCATION_ACCESS_MISSING", ipAddress);
        }

        UserAccountPinEntity credential = pinRepository.findByAccountIdAndUserIdForUpdate(accountId, userId)
                .orElseThrow(() -> verificationFailure(accountId, locationId, userId, deviceId, "PIN_NOT_CONFIGURED", ipAddress));
        if (credential.getStatus() != PinCredentialStatus.ACTIVE) {
            throw verificationFailure(accountId, locationId, userId, deviceId, "PIN_REVOKED", ipAddress);
        }

        Instant now = clock.instant();
        if (credential.getLockedUntil() != null && credential.getLockedUntil().isAfter(now)) {
            recordRateFailure(accountId, locationId, userId, deviceId, ipAddress, credential, "CREDENTIAL_LOCKED");
            auditService.record(PinAuditService.Event.builder()
                    .eventType(PinAuditEventType.PIN_LOGIN_FAILED)
                    .accountId(accountId)
                    .locationId(locationId)
                    .targetUserId(userId)
                    .actorType(AuditActorType.SYSTEM)
                    .deviceId(deviceId)
                    .failureCategory("CREDENTIAL_LOCKED")
                    .lockoutUntil(credential.getLockedUntil())
                    .credentialVersion(credential.getCredentialVersion())
                    .build());
            long retryAfter = Math.max(1, Duration.between(now, credential.getLockedUntil()).toSeconds());
            throw new PinApiException(
                    HttpStatus.LOCKED,
                    "PIN_LOCKED",
                    "PIN credential is locked",
                    credential.getLockedUntil(),
                    retryAfter
            );
        }

        boolean validPin;
        try {
            cryptoService.validatePin(pin);
            validPin = cryptoService.matchesOnlineHash(accountId, userId, pin, credential.getOnlinePinHash());
        } catch (IllegalArgumentException ignored) {
            validPin = false;
        }
        if (!validPin) {
            credential.setFailedAttempts(credential.getFailedAttempts() + 1);
            credential.setLastFailedAt(now);
            Duration lockDuration = lockoutPolicy.durationForFailure(credential.getFailedAttempts());
            credential.setLockoutLevel(lockoutPolicy.levelForFailure(credential.getFailedAttempts()));
            if (!lockDuration.isZero()) {
                credential.setLockedUntil(now.plus(lockDuration));
            }
            pinRepository.save(credential);
            recordRateFailure(accountId, locationId, userId, deviceId, ipAddress, credential, "PIN_MISMATCH");
            auditService.record(PinAuditService.Event.builder()
                    .eventType(lockDuration.isZero() ? PinAuditEventType.PIN_LOGIN_FAILED : PinAuditEventType.PIN_LOCKED)
                    .accountId(accountId)
                    .locationId(locationId)
                    .targetUserId(userId)
                    .actorType(AuditActorType.SYSTEM)
                    .deviceId(deviceId)
                    .failureCategory("PIN_MISMATCH")
                    .lockoutDurationSeconds(lockDuration.isZero() ? null : lockDuration.toSeconds())
                    .lockoutUntil(credential.getLockedUntil())
                    .credentialVersion(credential.getCredentialVersion())
                    .build());
            if (!lockDuration.isZero()) {
                throw new PinApiException(
                        HttpStatus.LOCKED,
                        "PIN_LOCKED",
                        "PIN credential is locked",
                        credential.getLockedUntil(),
                        lockDuration.toSeconds()
                );
            }
            throw new PinApiException(HttpStatus.UNAUTHORIZED, "PIN_VERIFICATION_FAILED", "PIN verification failed");
        }

        resetLockout(credential);
        pinRepository.save(credential);
        device.setLastSeenAt(now);
        deviceRepository.save(device);
        auditService.record(PinAuditService.Event.builder()
                .eventType(PinAuditEventType.PIN_LOGIN_SUCCEEDED)
                .accountId(accountId)
                .locationId(locationId)
                .targetUserId(userId)
                .actorType(AuditActorType.USER)
                .actorUserId(userId)
                .deviceId(deviceId)
                .credentialVersion(credential.getCredentialVersion())
                .build());
        String token = actionTokenService.issue(userId, accountId, locationId, deviceId, credential.getCredentialVersion());
        return new PinVerificationResponse(
                true,
                token,
                PinActionTokenService.TOKEN_LIFETIME.toSeconds(),
                user.getId(),
                user.getUserName(),
                accountId
        );
    }

    @Transactional(noRollbackFor = PinApiException.class)
    public PinVerificationResponse verifyOnlineByAccountPin(
            UUID accountId, UUID deviceId, String pin) {
        cryptoService.validatePin(pin);
        String digest = cryptoService.lookupDigest(accountId, pin);
        UUID userId = pinRepository.findByAccountIdAndPinLookupDigest(accountId, digest)
                .filter(credential -> credential.getStatus() == PinCredentialStatus.ACTIVE)
                .map(credential -> credential.getUser().getId())
                .orElseGet(UUID::randomUUID);
        return verifyOnline(accountId, null, userId, pin, deviceId);
    }

    @Transactional(noRollbackFor = PinApiException.class)
    public PinVerificationResponse verifyOnlineByDevicePin(UUID deviceId, String pin) {
        IpadDeviceEntity device = deviceRepository.findByIdAndActiveTrueWithScope(deviceId)
                .orElseThrow(() -> new PinApiException(
                        HttpStatus.UNAUTHORIZED, "PIN_VERIFICATION_FAILED", "PIN verification failed"));
        UUID accountId = device.getAccount().getId();
        return verifyOnlineByAccountPin(accountId, deviceId, pin);
    }

    @Transactional(readOnly = true)
    public PinStatusDto getPinStatus(UUID accountId, UUID userId) {
        authorizationService.requireCanManagePins(authorizationService.currentActorId(), accountId);
        requireActiveAccount(accountId);
        requireEligibleAccountUser(accountId, userId);
        return pinRepository.findByAccountIdAndUserId(accountId, userId)
                .map(this::toStatus)
                .orElse(new PinStatusDto(false, false, null, 0));
    }

    @Transactional(readOnly = true)
    public List<AccountUserDto> getAccountUsers(UUID accountId, UUID actorId) {
        authorizationService.requireCanManageAccount(actorId, accountId);
        AccountEntity account = accountRepository.findById(accountId)
                .orElseThrow(() -> new PinApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found"));
        if (!Boolean.TRUE.equals(account.getAccountActive()) || account.getDeletedAt() != null) {
            throw new PinApiException(HttpStatus.CONFLICT, "ACCOUNT_INACTIVE", "Account is inactive");
        }
        Map<UUID, UserAccountPinEntity> pins = new HashMap<>();
        for (UserAccountPinEntity pin : pinRepository.findAllByAccountId(accountId)) {
            pins.put(pin.getUser().getId(), pin);
        }
        return accountAccessRepository.findByAccount(account).stream()
                .map(access -> toAccountUser(access.getUser(), pins.get(access.getUser().getId())))
                .toList();
    }

    @Transactional
    public OfflinePinVerifierBundleDto buildOfflineVerifierBundle(UUID deviceId) {
        requireAuthenticatedDevice(deviceId);
        IpadDeviceEntity device = deviceRepository.findByIdForUpdate(deviceId)
                .orElseThrow(() -> new PinApiException(HttpStatus.UNAUTHORIZED, "INVALID_DEVICE", "Device not found"));
        if (!device.isActive()) {
            throw new PinApiException(HttpStatus.UNAUTHORIZED, "INVALID_DEVICE", "Device is revoked");
        }
        if (!Boolean.TRUE.equals(device.getAccount().getAccountActive())
                || device.getAccount().getDeletedAt() != null
                || (device.getLocation() != null
                    && (!Boolean.TRUE.equals(device.getLocation().getLocationActive())
                        || device.getLocation().getDeletedAt() != null))) {
            throw new PinApiException(HttpStatus.UNAUTHORIZED, "INVALID_DEVICE", "Device scope is inactive");
        }
        Instant now = clock.instant();
        List<OfflinePinVerifierUserDto> users = new ArrayList<>();
        for (UserAccountPinEntity credential : pinRepository.findAllActiveByAccountId(device.getAccount().getId())) {
            String verifier = cryptoService.decryptOfflineVerifier(
                    credential.getEncryptedOfflineVerifier(),
                    credential.getOfflineVerifierNonce(),
                    credential.getEncryptionKeyVersion()
            );
            users.add(new OfflinePinVerifierUserDto(
                    credential.getUser().getId(),
                    credential.getUser().getUserName(),
                    credential.getUser().getUserImage(),
                    credential.getPinLength(),
                    verifier,
                    credential.getCredentialVersion()
            ));
        }
        device.setBundleVersion(device.getBundleVersion() + 1);
        device.setLastSeenAt(now);
        deviceRepository.save(device);
        auditService.record(PinAuditService.Event.builder()
                .eventType(PinAuditEventType.PIN_BUNDLE_REFRESHED)
                .accountId(device.getAccount().getId())
                .locationId(device.getLocation() == null ? null : device.getLocation().getId())
                .actorType(AuditActorType.DEVICE)
                .deviceId(deviceId)
                .build());
        return new OfflinePinVerifierBundleDto(
                device.getAccount().getId(),
                device.getLocation() == null ? null : device.getLocation().getId(),
                now,
                now.plus(Duration.ofHours(offlineBundleHours)),
                device.getBundleVersion(),
                users
        );
    }

    private void applyPin(UserAccountPinEntity credential, String pin, String digest, UUID actorId, boolean replacing) {
        PinCryptoService.EncryptedVerifier offline = cryptoService.createEncryptedOfflineVerifier(pin);
        credential.setPinLookupDigest(digest);
        credential.setOnlinePinHash(cryptoService.createOnlineHash(
                credential.getAccount().getId(), credential.getUser().getId(), pin
        ));
        credential.setEncryptedOfflineVerifier(offline.ciphertext());
        credential.setOfflineVerifierNonce(offline.nonce());
        credential.setEncryptionKeyVersion(offline.keyVersion());
        credential.setPinLength(pin.length());
        credential.setStatus(PinCredentialStatus.ACTIVE);
        credential.setUpdatedBy(actorId);
        if (replacing) {
            credential.setCredentialVersion(credential.getCredentialVersion() + 1);
        }
        resetLockout(credential);
    }

    private UserAccountPinEntity newCredential(AccountEntity account, UserEntity user, UUID actorId) {
        UserAccountPinEntity credential = new UserAccountPinEntity();
        credential.setAccount(account);
        credential.setUser(user);
        credential.setCreatedBy(actorId);
        credential.setUpdatedBy(actorId);
        credential.setCredentialVersion(1);
        return credential;
    }

    private void rejectDigestOwnedByAnotherUser(UUID accountId, UUID userId, String digest) {
        pinRepository.findByAccountIdAndPinLookupDigest(accountId, digest).ifPresent(existing -> {
            if (!existing.getUser().getId().equals(userId)) {
                throw new PinApiException(
                        HttpStatus.CONFLICT,
                        "PIN_ALREADY_IN_USE",
                        "That PIN is already assigned within this account"
                );
            }
        });
    }

    private AccountEntity requireActiveAccountForUpdate(UUID accountId) {
        AccountEntity account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new PinApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found"));
        if (!Boolean.TRUE.equals(account.getAccountActive()) || account.getDeletedAt() != null) {
            throw new PinApiException(HttpStatus.CONFLICT, "ACCOUNT_INACTIVE", "Account is inactive");
        }
        return account;
    }

    private AccountEntity requireActiveAccount(UUID accountId) {
        AccountEntity account = accountRepository.findById(accountId)
                .orElseThrow(() -> new PinApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found"));
        if (!Boolean.TRUE.equals(account.getAccountActive()) || account.getDeletedAt() != null) {
            throw new PinApiException(HttpStatus.CONFLICT, "ACCOUNT_INACTIVE", "Account is inactive");
        }
        return account;
    }

    private UserEntity requireEligibleAccountUser(UUID accountId, UUID userId) {
        UserEntity user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new PinApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        if (!user.isUserActive()) {
            throw new PinApiException(HttpStatus.CONFLICT, "USER_INACTIVE", "User is inactive");
        }
        if (!accountAccessRepository.existsByUserIdAndAccountId(userId, accountId)) {
            throw new PinApiException(HttpStatus.BAD_REQUEST, "USER_NOT_IN_ACCOUNT", "User does not belong to account");
        }
        return user;
    }

    private PinApiException verificationFailure(
            UUID accountId,
            UUID locationId,
            UUID userId,
            UUID deviceId,
            String category,
            String ipAddress
    ) {
        recordRateFailure(accountId, locationId, userId, deviceId, ipAddress, null, category);
        auditService.record(PinAuditService.Event.builder()
                .eventType(PinAuditEventType.PIN_LOGIN_FAILED)
                .accountId(accountId)
                .locationId(locationId)
                .targetUserId(userId)
                .actorType(AuditActorType.SYSTEM)
                .deviceId(deviceId)
                .failureCategory(category)
                .build());
        return new PinApiException(HttpStatus.UNAUTHORIZED, "PIN_VERIFICATION_FAILED", "PIN verification failed");
    }

    private void recordRateFailure(
            UUID accountId,
            UUID locationId,
            UUID userId,
            UUID deviceId,
            String ipAddress,
            UserAccountPinEntity credential,
            String category
    ) {
        if (rateLimitService.recordFailure(accountId, ipAddress)) {
            auditService.record(PinAuditService.Event.builder()
                    .eventType(PinAuditEventType.PIN_LOGIN_FAILED)
                    .accountId(accountId)
                    .locationId(locationId)
                    .targetUserId(userId)
                    .actorType(AuditActorType.SYSTEM)
                    .deviceId(deviceId)
                    .failureCategory("ACCOUNT_THROTTLED:" + category)
                    .credentialVersion(credential == null ? null : credential.getCredentialVersion())
                    .build());
        }
    }

    private PinStatusDto toStatus(UserAccountPinEntity credential) {
        boolean configured = credential.getStatus() == PinCredentialStatus.ACTIVE;
        boolean locked = configured && credential.getLockedUntil() != null && credential.getLockedUntil().isAfter(clock.instant());
        return new PinStatusDto(configured, locked, locked ? credential.getLockedUntil() : null, credential.getCredentialVersion());
    }

    private AccountUserDto toAccountUser(UserEntity user, UserAccountPinEntity credential) {
        PinStatusDto status = credential == null
                ? new PinStatusDto(false, false, null, 0)
                : toStatus(credential);
        return new AccountUserDto(
                user.getId(), user.getUserName(), user.getUserEmail(), user.getUserImage(),
                user.isUserActive(), user.isFirstLogin(), user.isInvited(),
                user.getAccessRole().name(), user.getAppRole().name(), user.getCreatedAt(), user.getUpdatedAt(),
                status.pinConfigured(), status.pinLocked(), status.pinLockedUntil(), status.credentialVersion()
        );
    }

    private static void resetLockout(UserAccountPinEntity credential) {
        credential.setFailedAttempts(0);
        credential.setLockoutLevel(0);
        credential.setLockedUntil(null);
        credential.setLastFailedAt(null);
    }

    private static void requireAuthenticatedDevice(UUID deviceId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof DeviceAuthenticationPrincipal principal)
                || !principal.deviceId().equals(deviceId)) {
            throw new PinApiException(HttpStatus.UNAUTHORIZED, "INVALID_DEVICE", "Active device authentication is required");
        }
    }
}
