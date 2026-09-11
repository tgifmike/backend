package com.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_eula_acceptances", uniqueConstraints = @UniqueConstraint(
        name = "uk_user_eula_acceptance_user_version", columnNames = {"user_id", "eula_version"}))
@Getter
@Setter
@NoArgsConstructor
public class UserEulaAcceptanceEntity {
    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "eula_version", nullable = false)
    private Integer eulaVersion;

    @Column(name = "accepted_at", nullable = false)
    private Instant acceptedAt;

    @Column(name = "device_id")
    private UUID deviceId;

    @Column(name = "app_version")
    private String appVersion;

    @Column(name = "ip_address")
    private String ipAddress;
}
