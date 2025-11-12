package com.backend.backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
    @JsonBackReference
    private LineCheckStationEntity lineCheckStation;

    @ManyToOne
    @JoinColumn(name = "item_id")
    private ItemEntity item;

    private boolean isChecked; // e.g., checkmark status
    private String notes; // any notes added during check
    private Double temperature; // optional: record temp if relevant
}

