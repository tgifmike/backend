package com.backend.backend.entity;

import com.backend.backend.enums.PinCredentialStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(
        name = "user_account_pins",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_account_pins_account_user", columnNames = {"account_id", "user_id"}),
                @UniqueConstraint(name = "uk_user_account_pins_account_digest", columnNames = {"account_id", "pin_lookup_digest"})
        }
)
public class UserAccountPinEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "pin_lookup_digest", length = 43)
    @JsonIgnore
    private String pinLookupDigest;

    @Column(name = "online_pin_hash", length = 512)
    @JsonIgnore
    private String onlinePinHash;

    @Column(name = "encrypted_offline_verifier", columnDefinition = "TEXT")
    @JsonIgnore
    private String encryptedOfflineVerifier;

    @Column(name = "offline_verifier_nonce", length = 32)
    @JsonIgnore
    private String offlineVerifierNonce;

    @Column(name = "encryption_key_version")
    @JsonIgnore
    private Integer encryptionKeyVersion;

    @Column(name = "pin_length")
    private Integer pinLength;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PinCredentialStatus status = PinCredentialStatus.ACTIVE;

    @Column(name = "failed_attempts", nullable = false)
    @JsonIgnore
    private int failedAttempts;

    @Column(name = "lockout_level", nullable = false)
    @JsonIgnore
    private int lockoutLevel;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_failed_at")
    @JsonIgnore
    private Instant lastFailedAt;

    @Column(name = "credential_version", nullable = false)
    private long credentialVersion = 1;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "entity_version", nullable = false)
    @JsonIgnore
    private long entityVersion;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
