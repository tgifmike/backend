package com.backend.backend.service;

import com.backend.backend.dto.IpadEnrollmentRequest;
import com.backend.backend.dto.IpadEnrollmentResponse;
import com.backend.backend.entity.AccountEntity;
import com.backend.backend.entity.IpadDeviceEntity;
import com.backend.backend.entity.LocationEntity;
import com.backend.backend.enums.AuditActorType;
import com.backend.backend.enums.PinAuditEventType;
import com.backend.backend.exception.PinApiException;
import com.backend.backend.repositories.AccountRepository;
import com.backend.backend.repositories.IpadDeviceRepository;
import com.backend.backend.repositories.LocationRepository;
import com.backend.backend.security.DeviceAuthenticationPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.List;
import com.backend.backend.dto.IpadDeviceSummaryDto;

@Service
@RequiredArgsConstructor
public class IpadDeviceService {
    private final IpadDeviceRepository deviceRepository;
    private final AccountRepository accountRepository;
    private final LocationRepository locationRepository;
    private final AccountAuthorizationService authorizationService;
    private final PinAuditService auditService;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public IpadEnrollmentResponse enroll(IpadEnrollmentRequest request, UUID actorId) {
        authorizationService.requireCanManageAccount(actorId, request.accountId());
        AccountEntity account = requireActiveAccount(request.accountId());
        LocationEntity location = request.locationId() == null
                ? null
                : requireActiveLocation(request.locationId(), request.accountId());
        validatePublicKey(request.devicePublicKey());

        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        IpadDeviceEntity device = new IpadDeviceEntity();
        device.setAccount(account);
        device.setLocation(location);
        device.setDeviceName(request.deviceName().trim());
        device.setDevicePublicKey(request.devicePublicKey().trim());
        device.setDeviceTokenHash(hashToken(rawToken));
        device.setActive(true);
        device.setEnrolledAt(clock.instant());
        device.setEnrolledBy(actorId);
        device = deviceRepository.save(device);

        auditService.record(PinAuditService.Event.builder()
                .eventType(PinAuditEventType.IPAD_DEVICE_ENROLLED)
                .accountId(account.getId())
                .locationId(location == null ? null : location.getId())
                .actorUserId(actorId)
                .actorType(AuditActorType.USER)
                .deviceId(device.getId())
                .build());

        return new IpadEnrollmentResponse(device.getId(), rawToken);
    }

    @Transactional(readOnly = true)
    public List<IpadDeviceSummaryDto> listForAccount(UUID accountId, UUID actorId) {
        authorizationService.requireCanManageAccount(actorId, accountId);
        return deviceRepository.findAllByAccountId(accountId).stream()
                .map(IpadDeviceSummaryDto::fromEntity)
                .toList();
    }

    @Transactional
    public void revoke(UUID deviceId, UUID actorId) {
        IpadDeviceEntity device = deviceRepository.findByIdForUpdate(deviceId)
                .orElseThrow(() -> new PinApiException(HttpStatus.NOT_FOUND, "DEVICE_NOT_FOUND", "iPad device not found"));
        authorizationService.requireCanManageAccount(actorId, device.getAccount().getId());
        if (device.isActive()) {
            device.setActive(false);
            device.setRevokedAt(clock.instant());
            device.setRevokedBy(actorId);
            deviceRepository.save(device);
            auditService.record(PinAuditService.Event.builder()
                    .eventType(PinAuditEventType.IPAD_DEVICE_REVOKED)
                    .accountId(device.getAccount().getId())
                    .locationId(device.getLocation() == null ? null : device.getLocation().getId())
                    .actorUserId(actorId)
                    .actorType(AuditActorType.USER)
                    .deviceId(device.getId())
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public DeviceAuthenticationPrincipal authenticate(UUID deviceId, String rawToken) {
        IpadDeviceEntity device = deviceRepository.findByIdAndActiveTrueWithScope(deviceId)
                .orElseThrow(() -> new PinApiException(HttpStatus.UNAUTHORIZED, "INVALID_DEVICE", "Device authentication failed"));
        if (!constantTimeEquals(device.getDeviceTokenHash(), hashToken(rawToken))) {
            throw new PinApiException(HttpStatus.UNAUTHORIZED, "INVALID_DEVICE", "Device authentication failed");
        }
        if (!Boolean.TRUE.equals(device.getAccount().getAccountActive())
                || device.getAccount().getDeletedAt() != null
                || (device.getLocation() != null
                    && (!Boolean.TRUE.equals(device.getLocation().getLocationActive())
                        || device.getLocation().getDeletedAt() != null))) {
            throw new PinApiException(HttpStatus.UNAUTHORIZED, "INVALID_DEVICE", "Device authentication failed");
        }
        return new DeviceAuthenticationPrincipal(
                device.getId(),
                device.getAccount().getId(),
                device.getLocation() == null ? null : device.getLocation().getId()
        );
    }

    @Transactional(readOnly = true)
    public IpadDeviceEntity requireActiveDevice(UUID deviceId) {
        return deviceRepository.findByIdAndActiveTrueWithScope(deviceId)
                .orElseThrow(() -> new PinApiException(HttpStatus.UNAUTHORIZED, "INVALID_DEVICE", "Device is not active"));
    }

    private AccountEntity requireActiveAccount(UUID accountId) {
        AccountEntity account = accountRepository.findById(accountId)
                .orElseThrow(() -> new PinApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found"));
        if (!Boolean.TRUE.equals(account.getAccountActive()) || account.getDeletedAt() != null) {
            throw new PinApiException(HttpStatus.CONFLICT, "ACCOUNT_INACTIVE", "Account is inactive");
        }
        return account;
    }

    private LocationEntity requireActiveLocation(UUID locationId, UUID accountId) {
        LocationEntity location = locationRepository.findById(locationId)
                .orElseThrow(() -> new PinApiException(HttpStatus.NOT_FOUND, "LOCATION_NOT_FOUND", "Location not found"));
        if (!location.getAccount().getId().equals(accountId)) {
            throw new PinApiException(HttpStatus.BAD_REQUEST, "LOCATION_ACCOUNT_MISMATCH", "Location does not belong to account");
        }
        if (!Boolean.TRUE.equals(location.getLocationActive()) || location.getDeletedAt() != null) {
            throw new PinApiException(HttpStatus.CONFLICT, "LOCATION_INACTIVE", "Location is inactive");
        }
        return location;
    }

    public static String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return "";
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                actual.getBytes(StandardCharsets.US_ASCII)
        );
    }

    public static void validatePublicKey(String encodedKey) {
        try {
            byte[] bytes;
            try {
                bytes = Base64.getDecoder().decode(encodedKey.trim());
            } catch (IllegalArgumentException ignored) {
                bytes = Base64.getUrlDecoder().decode(encodedKey.trim());
            }
            KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(bytes));
        } catch (Exception ex) {
            throw new PinApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_DEVICE_PUBLIC_KEY",
                    "devicePublicKey must be a Base64-encoded Ed25519 X.509 public key"
            );
        }
    }
}
