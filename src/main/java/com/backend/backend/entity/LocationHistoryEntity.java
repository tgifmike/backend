package com.backend.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "location_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationHistoryEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "location_name", nullable = false)
    private String locationName;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "location_id")
    @JsonIgnore
    private LocationEntity location;


    @Column(name = "change_type", nullable = false)
    private String changeType; // "CREATED", "UPDATED", "DELETED"

    @Column(name = "change_at", nullable = false)
    private Instant changeAt;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "changed_by_name")
    private String changedByName;

    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues; // JSON string

    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues; // JSON string
}




