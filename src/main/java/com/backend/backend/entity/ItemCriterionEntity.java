package com.backend.backend.entity;

import com.backend.backend.enums.ResponseType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "item_criteria",
        indexes = @Index(name = "idx_item_criteria_item_sort", columnList = "item_id, sort_order")
)
public class ItemCriterionEntity {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    @JsonBackReference("item-criteria")
    private ItemEntity item;

    @Column(nullable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "response_type", nullable = false)
    private ResponseType responseType;

    @Builder.Default
    @Column(nullable = false)
    private Boolean required = false;

    @Builder.Default
    @Column(name = "require_notes_on_failure", nullable = false)
    private Boolean requireNotesOnFailure = false;

    @Column(name = "min_value")
    private Double minValue;

    @Column(name = "max_value")
    private Double maxValue;

    private String unit;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
}
