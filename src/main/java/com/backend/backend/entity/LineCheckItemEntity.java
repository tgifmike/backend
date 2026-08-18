package com.backend.backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "line_check_items")
public class LineCheckItemEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "line_check_station_id")
    @JsonBackReference("LCSE")
    private LineCheckStationEntity lineCheckStation;

    @ManyToOne
    @JoinColumn(name = "item_id")
    @JsonIgnore // optional
    private ItemEntity item;

    @ManyToOne
    @JoinColumn(name = "station_id")
    @JsonIgnore // optional, prevent cycles
    private StationEntity station;


    @Column(name = "is_item_checked", nullable = false)
    private boolean isItemChecked;

    @Column(name = "is_checked", nullable = false)
    private boolean isChecked;

    @Column(name = "is_missing", nullable = false)
    private boolean isMissing = false;

    @Column(name = "notes")
    private String itemNotes;

    @Column(name = "observations")
    private String observations;

    @Column(name = "temperature")
    private Double temperature;

    @OneToMany(
            mappedBy = "lineCheckItem",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JsonManagedReference
    private List<LineCheckPhotoEntity> photos = new ArrayList<>();

    @Column(name = "corrective_action")
    private String correctiveAction;

    @Column(name = "corrected_at")
    private Instant correctedAt;

    @Column(name = "corrected_by")
    private UUID correctedBy;


    @Column(name = "requires_correction", nullable = false)
    private boolean requiresCorrection = false;

    @Column(name = "is_corrected", nullable = false)
    private boolean isCorrected = false;

}
