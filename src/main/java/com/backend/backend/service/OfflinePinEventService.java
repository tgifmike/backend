package com.backend.backend.service;

import com.backend.backend.dto.OfflinePinEventBatchResponse;
import com.backend.backend.dto.OfflinePinEventDto;
import com.backend.backend.entity.IpadDeviceEntity;
import com.backend.backend.entity.LineCheckEntity;
import com.backend.backend.entity.UserAccountPinEntity;
import com.backend.backend.enums.AuditActorType;
import com.backend.backend.enums.OfflineVerificationStatus;
import com.backend.backend.enums.PinAuditEventType;
import com.backend.backend.exception.PinApiException;
import com.backend.backend.repositories.IpadDeviceRepository;
import com.backend.backend.repositories.LineCheckRepository;
import com.backend.backend.repositories.PinAuthenticationAuditRepository;
import com.backend.backend.repositories.UserAccountPinRepository;
import com.backend.backend.security.DeviceAuthenticationPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OfflinePinEventService {
    private static final Set<PinAuditEventType> SUPPORTED = EnumSet.of(
            PinAuditEventType.PIN_OFFLINE_SUCCESS,
            PinAuditEventType.PIN_OFFLINE_FAILURE,
            PinAuditEventType.PIN_OFFLINE_LOCKED,
            PinAuditEventType.PIN_BUNDLE_REFRESHED,
            PinAuditEventType.PIN_VERIFIER_STALE
    );

    private final IpadDeviceRepository deviceRepository;
    private final UserAccountPinRepository pinRepository;
    private final PinAuthenticationAuditRepository auditRepository;
    private final LineCheckRepository lineCheckRepository;
    private final PinAuditService auditService;
    private final Clock clock;

    @Transactional
    public OfflinePinEventBatchResponse acceptBatch(UUID deviceId, List<OfflinePinEventDto> events) {
        requireAuthenticatedDevice(deviceId);
        IpadDeviceEntity device = deviceRepository.findByIdForUpdate(deviceId)
                .orElseThrow(() -> new PinApiException(HttpStatus.UNAUTHORIZED, "INVALID_DEVICE", "Device not found"));
        if (!device.isActive()) {
            throw new PinApiException(HttpStatus.UNAUTHORIZED, "INVALID_DEVICE", "Device is revoked");
        }

        int accepted = 0;
        int duplicates = 0;
        int stale = 0;
        Set<UUID> seenInBatch = new HashSet<>();
        PublicKey publicKey = decodePublicKey(device.getDevicePublicKey());

        for (OfflinePinEventDto event : events) {
            validateEventScope(device, event);
            verifySignature(publicKey, event);
            if (!seenInBatch.add(event.eventId()) || auditRepository.existsBySourceEventId(event.eventId())) {
                duplicates++;
                continue;
            }

            long currentVersion = pinRepository.findByAccountIdAndUserId(event.accountId(), event.userId())
                    .map(UserAccountPinEntity::getCredentialVersion)
                    .orElse(0L);
            boolean staleCredential = currentVersion != event.credentialVersion();

            auditService.record(PinAuditService.Event.builder()
                    .eventType(event.eventType())
                    .sourceEventId(event.eventId())
                    .sequenceNumber(event.sequenceNumber())
                    .accountId(event.accountId())
                    .locationId(event.locationId())
                    .targetUserId(event.userId())
                    .actorType(AuditActorType.DEVICE)
                    .deviceId(deviceId)
                    .lineCheckId(event.lineCheckId())
                    .occurredAt(event.occurredAt())
                    .lockoutDurationSeconds(lockoutDuration(event))
                    .lockoutUntil(event.lockoutUntil())
                    .credentialVersion(event.credentialVersion())
                    .build());

            if (event.lineCheckId() != null) {
                recordLineCheckAuthentication(device, event, staleCredential);
            }
            if (staleCredential) {
                stale++;
                auditService.record(PinAuditService.Event.builder()
                        .eventType(PinAuditEventType.PIN_VERIFIER_STALE)
                        .accountId(event.accountId())
                        .locationId(event.locationId())
                        .targetUserId(event.userId())
                        .actorType(AuditActorType.DEVICE)
                        .deviceId(deviceId)
                        .lineCheckId(event.lineCheckId())
                        .occurredAt(event.occurredAt())
                        .failureCategory("CURRENT_VERSION_" + currentVersion)
                        .credentialVersion(event.credentialVersion())
                        .build());
            }
            accepted++;
        }

        device.setLastSeenAt(clock.instant());
        deviceRepository.save(device);
        return new OfflinePinEventBatchResponse(accepted, duplicates, stale);
    }

    public static String canonicalPayload(OfflinePinEventDto event) {
        return String.join("|",
                event.eventId().toString(),
                Long.toString(event.sequenceNumber()),
                event.eventType().name(),
                event.accountId().toString(),
                event.locationId().toString(),
                event.userId().toString(),
                Long.toString(event.credentialVersion()),
                event.occurredAt().toString(),
                event.lockoutUntil() == null ? "" : event.lockoutUntil().toString(),
                event.lineCheckId() == null ? "" : event.lineCheckId().toString()
        );
    }

    private void validateEventScope(IpadDeviceEntity device, OfflinePinEventDto event) {
        if (!SUPPORTED.contains(event.eventType())) {
            throw new PinApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_PIN_EVENT", "Unsupported offline PIN event type");
        }
        if (!device.getAccount().getId().equals(event.accountId())
                || (device.getLocation() != null
                    && !device.getLocation().getId().equals(event.locationId()))) {
            throw new PinApiException(HttpStatus.BAD_REQUEST, "DEVICE_EVENT_SCOPE_MISMATCH", "Event is outside the device scope");
        }
    }

    private void verifySignature(PublicKey publicKey, OfflinePinEventDto event) {
        try {
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(publicKey);
            verifier.update(canonicalPayload(event).getBytes(StandardCharsets.UTF_8));
            byte[] signature;
            try {
                signature = Base64.getDecoder().decode(event.deviceSignature());
            } catch (IllegalArgumentException ignored) {
                signature = Base64.getUrlDecoder().decode(event.deviceSignature());
            }
            if (!verifier.verify(signature)) {
                throw new PinApiException(HttpStatus.UNAUTHORIZED, "INVALID_DEVICE_SIGNATURE", "Offline event signature is invalid");
            }
        } catch (PinApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PinApiException(HttpStatus.UNAUTHORIZED, "INVALID_DEVICE_SIGNATURE", "Offline event signature is invalid");
        }
    }

    private void recordLineCheckAuthentication(
            IpadDeviceEntity device,
            OfflinePinEventDto event,
            boolean staleCredential
    ) {
        LineCheckEntity lineCheck = lineCheckRepository.findById(event.lineCheckId())
                .orElseThrow(() -> new PinApiException(HttpStatus.BAD_REQUEST, "LINE_CHECK_NOT_FOUND", "Line check not found"));
        boolean correctUser = lineCheck.getUser() != null && lineCheck.getUser().getId().equals(event.userId());
        boolean correctLocation = lineCheck.getStations().stream().allMatch(stationCheck ->
                stationCheck.getStation() != null
                        && stationCheck.getStation().getLocation() != null
                        && stationCheck.getStation().getLocation().getId().equals(event.locationId())
        );
        if (!correctUser || !correctLocation) {
            throw new PinApiException(
                    HttpStatus.BAD_REQUEST,
                    "LINE_CHECK_EVENT_SCOPE_MISMATCH",
                    "Line check does not match the signed event scope"
            );
        }
        lineCheck.setAuthDeviceId(device.getId());
        lineCheck.setAuthAccountId(event.accountId());
        lineCheck.setAuthLocationId(event.locationId());
        lineCheck.setAuthUserId(event.userId());
        lineCheck.setAuthCredentialVersion(event.credentialVersion());
        lineCheck.setAuthVerifiedAt(event.occurredAt());
        lineCheck.setAuthLocalEventId(event.eventId());
        lineCheck.setVerificationStatus(
                staleCredential ? OfflineVerificationStatus.STALE_CREDENTIAL : OfflineVerificationStatus.CURRENT
        );
        lineCheckRepository.save(lineCheck);
    }

    private static PublicKey decodePublicKey(String encoded) {
        try {
            byte[] bytes;
            try {
                bytes = Base64.getDecoder().decode(encoded);
            } catch (IllegalArgumentException ignored) {
                bytes = Base64.getUrlDecoder().decode(encoded);
            }
            return KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(bytes));
        } catch (Exception ex) {
            throw new PinApiException(HttpStatus.UNAUTHORIZED, "INVALID_DEVICE_SIGNATURE", "Device public key is invalid");
        }
    }

    private static Long lockoutDuration(OfflinePinEventDto event) {
        if (event.lockoutUntil() == null || !event.lockoutUntil().isAfter(event.occurredAt())) {
            return null;
        }
        return Duration.between(event.occurredAt(), event.lockoutUntil()).toSeconds();
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
