package com.backend.backend.entity;

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

