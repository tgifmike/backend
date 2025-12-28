package com.backend.backend.entity;

import com.backend.backend.enums.HistoryType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "station_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StationHistoryEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "station_name", nullable = false)
    private String stationName;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "station_id")
    @JsonIgnore
    private StationEntity station;

    private Boolean stationActive;

    private Integer stationSortOrder;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

//    @Column(name = "change_type", nullable = false)
//    private String changeType; // "CREATED", "UPDATED", "DELETED"

    @Column(name = "change_at", nullable = false)
    private Instant changeAt;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "changed_by_name")
    private String changedByName;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false)
    private HistoryType changeType;

    @ElementCollection
    @CollectionTable(
            name = "station_history_old_values",
            joinColumns = @JoinColumn(name = "history_id")
    )
    @MapKeyColumn(name = "field_name")
    @Column(name = "old_value")
    private Map<String, String> oldValues = new HashMap<>();

}

