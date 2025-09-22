package com.backend.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name= "locations")
public class LocationEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    private String locationName;
    private String locationStreet;
    private String locationTown;
    private String locationState;
    private String locationTimezone;
    private String locationZipCode;
    private Double locationLatitude;
    private Double locationLongitude;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private AccountEntity account;

    private boolean locationActive = true;

    @Column(name = "geocoded_from_zip_fallback")
    private Boolean geocodedFromZipFallback;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
