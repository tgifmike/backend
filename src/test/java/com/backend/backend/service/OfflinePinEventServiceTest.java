package com.backend.backend.service;

import com.backend.backend.dto.OfflinePinEventBatchResponse;
import com.backend.backend.dto.OfflinePinEventDto;
import com.backend.backend.entity.AccountEntity;
import com.backend.backend.entity.IpadDeviceEntity;
import com.backend.backend.entity.LineCheckEntity;
import com.backend.backend.entity.LocationEntity;
import com.backend.backend.entity.UserAccountPinEntity;
import com.backend.backend.entity.UserEntity;
import com.backend.backend.enums.OfflineVerificationStatus;
import com.backend.backend.enums.PinAuditEventType;
import com.backend.backend.exception.PinApiException;
import com.backend.backend.repositories.IpadDeviceRepository;
import com.backend.backend.repositories.LineCheckRepository;
import com.backend.backend.repositories.PinAuthenticationAuditRepository;
import com.backend.backend.repositories.UserAccountPinRepository;
import com.backend.backend.security.DeviceAuthenticationPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OfflinePinEventServiceTest {
    private final UUID deviceId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();
    private final UUID locationId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final IpadDeviceRepository devices = mock(IpadDeviceRepository.class);
    private final UserAccountPinRepository pins = mock(UserAccountPinRepository.class);
    private final PinAuthenticationAuditRepository audits = mock(PinAuthenticationAuditRepository.class);
    private final LineCheckRepository lineChecks = mock(LineCheckRepository.class);
    private final PinAuditService auditService = mock(PinAuditService.class);
    private KeyPair keyPair;
    private IpadDeviceEntity device;
    private OfflinePinEventService service;

    @BeforeEach
    void setUp() throws Exception {
        keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        AccountEntity account = new AccountEntity();
        account.setId(accountId);
        LocationEntity location = new LocationEntity();
        location.setId(locationId);
        location.setAccount(account);
        device = new IpadDeviceEntity();
        device.setId(deviceId);
        device.setAccount(account);
        device.setLocation(location);
        device.setActive(true);
        device.setDevicePublicKey(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        when(devices.findByIdForUpdate(deviceId)).thenReturn(Optional.of(device));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new DeviceAuthenticationPrincipal(deviceId, accountId, locationId),
                null,
                List.of(new SimpleGrantedAuthority("DEVICE_AUTH"))
        ));
        service = new OfflinePinEventService(
                devices,
                pins,
                audits,
                lineChecks,
                auditService,
                Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"), ZoneOffset.UTC)
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void duplicateOfflineEventIdsAreIdempotent() throws Exception {
        OfflinePinEventDto event = signedEvent(UUID.randomUUID(), 3, null);
        UserAccountPinEntity credential = new UserAccountPinEntity();
        credential.setCredentialVersion(3);
        when(pins.findByAccountIdAndUserId(accountId, userId)).thenReturn(Optional.of(credential));
        when(audits.existsBySourceEventId(event.eventId())).thenReturn(false);

        OfflinePinEventBatchResponse response = service.acceptBatch(deviceId, List.of(event, event));

        assertThat(response.accepted()).isEqualTo(1);
        assertThat(response.duplicates()).isEqualTo(1);
        assertThat(response.staleCredentials()).isZero();
    }

    @Test
    void staleCredentialVersionFlagsButPreservesLineCheck() throws Exception {
        UUID lineCheckId = UUID.randomUUID();
        OfflinePinEventDto event = signedEvent(UUID.randomUUID(), 2, lineCheckId);
        UserAccountPinEntity credential = new UserAccountPinEntity();
        credential.setCredentialVersion(3);
        LineCheckEntity lineCheck = new LineCheckEntity();
        lineCheck.setId(lineCheckId);
        UserEntity lineCheckUser = new UserEntity();
        lineCheckUser.setId(userId);
        lineCheck.setUser(lineCheckUser);
        when(pins.findByAccountIdAndUserId(accountId, userId)).thenReturn(Optional.of(credential));
        when(audits.existsBySourceEventId(event.eventId())).thenReturn(false);
        when(lineChecks.findById(lineCheckId)).thenReturn(Optional.of(lineCheck));

        OfflinePinEventBatchResponse response = service.acceptBatch(deviceId, List.of(event));

        assertThat(response.accepted()).isEqualTo(1);
        assertThat(response.staleCredentials()).isEqualTo(1);
        assertThat(lineCheck.getVerificationStatus()).isEqualTo(OfflineVerificationStatus.STALE_CREDENTIAL);
        assertThat(lineCheck.getAuthLocalEventId()).isEqualTo(event.eventId());
        assertThat(lineCheck.getAuthCredentialVersion()).isEqualTo(2);
    }

    @Test
    void revokedDeviceCannotUploadEvents() throws Exception {
        device.setActive(false);
        OfflinePinEventDto event = signedEvent(UUID.randomUUID(), 1, null);
        assertThatThrownBy(() -> service.acceptBatch(deviceId, List.of(event)))
                .isInstanceOf(PinApiException.class)
                .extracting("code")
                .isEqualTo("INVALID_DEVICE");
    }

    private OfflinePinEventDto signedEvent(UUID eventId, long credentialVersion, UUID lineCheckId) throws Exception {
        OfflinePinEventDto unsigned = new OfflinePinEventDto(
                eventId,
                1,
                PinAuditEventType.PIN_OFFLINE_SUCCESS,
                accountId,
                locationId,
                userId,
                credentialVersion,
                Instant.parse("2026-09-01T11:55:00Z"),
                null,
                lineCheckId,
                "unsigned"
        );
        Signature signature = Signature.getInstance("Ed25519");
        signature.initSign(keyPair.getPrivate());
        signature.update(OfflinePinEventService.canonicalPayload(unsigned).getBytes(StandardCharsets.UTF_8));
        String encoded = Base64.getEncoder().encodeToString(signature.sign());
        return new OfflinePinEventDto(
                unsigned.eventId(), unsigned.sequenceNumber(), unsigned.eventType(), unsigned.accountId(),
                unsigned.locationId(), unsigned.userId(), unsigned.credentialVersion(), unsigned.occurredAt(),
                unsigned.lockoutUntil(), unsigned.lineCheckId(), encoded
        );
    }
}
