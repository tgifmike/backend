package com.backend.backend.service;

import com.backend.backend.entity.PinAuthenticationAuditEntity;
import com.backend.backend.enums.AuditActorType;
import com.backend.backend.enums.PinAuditEventType;
import com.backend.backend.repositories.PinAuthenticationAuditRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PinAuditService {
    private final PinAuthenticationAuditRepository repository;
    private final AuditRequestMetadataProvider metadataProvider;
    private final Clock clock;

    public PinAuthenticationAuditEntity record(Event event) {
        AuditRequestMetadataProvider.AuditMetadata metadata = metadataProvider.current();
        Instant now = clock.instant();
        PinAuthenticationAuditEntity audit = new PinAuthenticationAuditEntity();
        audit.setEventType(event.eventType());
        audit.setSourceEventId(event.sourceEventId());
        audit.setSequenceNumber(event.sequenceNumber());
        audit.setAccountId(event.accountId());
        audit.setLocationId(event.locationId());
        audit.setTargetUserId(event.targetUserId());
        audit.setActorUserId(event.actorUserId());
        audit.setActorType(event.actorType());
        audit.setDeviceId(event.deviceId());
        audit.setLineCheckId(event.lineCheckId());
        audit.setOccurredAt(event.occurredAt() == null ? now : event.occurredAt());
        audit.setReceivedAt(now);
        audit.setIpAddress(metadata.ipAddress());
        audit.setUserAgent(metadata.userAgent());
        audit.setCorrelationId(metadata.correlationId());
        audit.setFailureCategory(event.failureCategory());
        audit.setLockoutDurationSeconds(event.lockoutDurationSeconds());
        audit.setLockoutUntil(event.lockoutUntil());
        audit.setCredentialVersion(event.credentialVersion());
        return repository.save(audit);
    }

    @Builder
    public record Event(
            PinAuditEventType eventType,
            UUID sourceEventId,
            Long sequenceNumber,
            UUID accountId,
            UUID locationId,
            UUID targetUserId,
            UUID actorUserId,
            AuditActorType actorType,
            UUID deviceId,
            UUID lineCheckId,
            Instant occurredAt,
            String failureCategory,
            Long lockoutDurationSeconds,
            Instant lockoutUntil,
            Long credentialVersion
    ) {
    }
}
