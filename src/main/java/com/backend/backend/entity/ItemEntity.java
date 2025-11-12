package com.backend.backend.entity;

import com.backend.backend.config.ItemTempCategory;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@Table(name= "items")
public class ItemEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String itemName;
    private String shelfLife;
    private String panSize;

    private String toolName;
    @JsonProperty("isTool")
    private boolean isTool;

    private String portionSize;
    @JsonProperty("isPortionSize")
    private boolean isPortioned;

    private double itemTemperature;
    @JsonProperty("isTempTaken")
    private boolean isTempTaken;

    @Enumerated(EnumType.STRING)
    private ItemTempCategory tempCategory; // FROZEN, REFRIGERATED, ROOM_TEMP, HOT_HOLDING

    private Double minTemp;
    private Double maxTemp;

    @JsonProperty("isCheckMark")
    private boolean isCheckMark;

    private String itemNotes;

    private String lineCheckNotes;

    private boolean itemActive = true;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    @JsonBackReference
    private StationEntity station;




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
