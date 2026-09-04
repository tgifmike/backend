package com.backend.backend.entity;

import com.backend.backend.enums.OfflineVerificationStatus;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "line_checks")
public class LineCheckEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id") // optional: the person performing the line check
    private UserEntity user;

    @Column(name = "check_time", nullable = false)
    private Instant checkTime;

    @OneToMany(mappedBy = "lineCheck", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("lineCheckE")
    private Set<LineCheckStationEntity> stations = new HashSet<>();

    @Column(name = "completed_at")
    private Instant completedAt;

    public boolean isCompleted() {
        return completedAt != null;
    }


    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "auth_device_id")
    private UUID authDeviceId;

    @Column(name = "auth_account_id")
    private UUID authAccountId;

    @Column(name = "auth_location_id")
    private UUID authLocationId;

    @Column(name = "auth_user_id")
    private UUID authUserId;

    @Column(name = "auth_credential_version")
    private Long authCredentialVersion;

    @Column(name = "auth_verified_at")
    private Instant authVerifiedAt;

    @Column(name = "auth_local_event_id", unique = true)
    private UUID authLocalEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", length = 32)
    private OfflineVerificationStatus verificationStatus;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now(); // always UTC
        createdAt = now;
        updatedAt = now;
        checkTime = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
