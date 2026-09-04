package com.backend.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "pin_auth_throttles",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_pin_auth_throttle_scope",
                columnNames = {"scope_type", "account_id", "ip_address"}
        )
)
public class PinAuthThrottleEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "scope_type", nullable = false, length = 16)
    private String scopeType;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "ip_address", nullable = false, length = 64)
    private String ipAddress;

    @Column(name = "window_started_at", nullable = false)
    private Instant windowStartedAt;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(name = "blocked_until")
    private Instant blockedUntil;

    @Version
    @Column(nullable = false)
    private long version;
}
