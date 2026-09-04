package com.backend.backend.entity;

import com.backend.backend.enums.AuditActorType;
import com.backend.backend.enums.PinAuditEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pin_authentication_audit")
public class PinAuthenticationAuditEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 48)
    private PinAuditEventType eventType;

    @Column(name = "source_event_id", unique = true)
    private UUID sourceEventId;

    @Column(name = "sequence_number")
    private Long sequenceNumber;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "target_user_id")
    private UUID targetUserId;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 16)
    private AuditActorType actorType;

    @Column(name = "device_id")
    private UUID deviceId;

    @Column(name = "line_check_id")
    private UUID lineCheckId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "correlation_id", length = 128)
    private String correlationId;

    @Column(name = "failure_category", length = 64)
    private String failureCategory;

    @Column(name = "lockout_duration_seconds")
    private Long lockoutDurationSeconds;

    @Column(name = "lockout_until")
    private Instant lockoutUntil;

    @Column(name = "credential_version")
    private Long credentialVersion;
}
