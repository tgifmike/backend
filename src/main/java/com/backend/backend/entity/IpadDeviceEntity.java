package com.backend.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ipad_devices")
public class IpadDeviceEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "location_id")
    private LocationEntity location;

    @Column(name = "device_name", nullable = false)
    private String deviceName;

    @Column(name = "device_token_hash", nullable = false, unique = true, length = 43)
    @JsonIgnore
    private String deviceTokenHash;

    @Column(name = "device_public_key", nullable = false, columnDefinition = "TEXT")
    @JsonIgnore
    private String devicePublicKey;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "enrolled_at", nullable = false)
    private Instant enrolledAt;

    @Column(name = "enrolled_by", nullable = false)
    private UUID enrolledBy;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_by")
    private UUID revokedBy;

    @Column(name = "bundle_version", nullable = false)
    private long bundleVersion;

    @Version
    @Column(nullable = false)
    private long version;
}
