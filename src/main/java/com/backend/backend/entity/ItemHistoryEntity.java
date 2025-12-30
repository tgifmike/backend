package com.backend.backend.entity;

import com.backend.backend.enums.HistoryType;
import com.backend.backend.enums.ItemTempCategory;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "item_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemHistoryEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID itemId;

    @Column(name = "station_id", insertable = false, updatable = false)
    private UUID stationId;


    private String itemName;
    private String shelfLife;
    private String panSize;

    private String toolName;
    @JsonProperty("isTool")
    private Boolean isTool;

    private String portionSize;
    @JsonProperty("isPortioned")
    private Boolean isPortioned;

    private Double itemTemperature;
    @JsonProperty("isTempTaken")
    private Boolean isTempTaken;

    @Enumerated(EnumType.STRING)
    private ItemTempCategory tempCategory; // FROZEN, REFRIGERATED, ROOM_TEMP, HOT_HOLDING

    private Double minTemp;
    private Double maxTemp;

    @JsonProperty("isCheckMark")
    private Boolean isCheckMark;

    private Boolean isItemChecked;

    @Column(name = "item_notes")
    private String templateNotes;


    private String lineCheckNotes;

    private Boolean itemActive = true;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    @JsonBackReference("station-items")
    private StationEntity station;

    private Instant changeAt;

    @Column(nullable = false)
    private UUID changedBy;

    @Column(nullable = false)
    private String changedByName; // store the username at the time of change

    @Enumerated(EnumType.STRING)
    private HistoryType changeType;


    // 📝 Store old values
    @ElementCollection
    @CollectionTable(
            name = "item_history_old_values",
            joinColumns = @JoinColumn(name = "history_id")
    )

    @MapKeyColumn(name = "field_name")
    @Column(name = "old_value")
    private Map<String, String> oldValues = new HashMap<>();
}
