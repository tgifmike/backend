package com.backend.backend.entity;

import com.backend.backend.enums.ItemTempCategory;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name= "items")
@SQLDelete(sql = "UPDATE items SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
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

    private Boolean itemChecked;

    @Column(name = "item_notes")
    private String templateNotes;


    private String lineCheckNotes;

    private Boolean itemActive = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    @JsonBackReference("station-items")
    private StationEntity station;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;

    //@LastModifiedDate
    @Column(name = "deleted_at")
    private Instant deletedAt;

    //@LastModifiedBy
    @Column(name = "deleted_by")
    private UUID deletedBy;
}
