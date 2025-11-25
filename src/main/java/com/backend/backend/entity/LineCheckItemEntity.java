package com.backend.backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

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


    @Column(name = "is_item_checked")
    private boolean isItemChecked;

    @Column(name = "is_checked")
    private boolean isChecked;

    @Column(name = "notes")
    private String itemNotes;

    @Column(name = "observations")
    private String observations;

    @Column(name = "temperature")
    private Double temperature;

}
