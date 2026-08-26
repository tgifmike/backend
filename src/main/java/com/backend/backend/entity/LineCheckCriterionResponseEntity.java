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
        name = "line_check_criterion_responses",
        indexes = @Index(
                name = "idx_line_check_criterion_responses_item_sort",
                columnList = "line_check_item_id, sort_order"
        )
)
public class LineCheckCriterionResponseEntity {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "line_check_item_id", nullable = false)
    @JsonBackReference("line-check-item-criterion-responses")
    private LineCheckItemEntity lineCheckItem;

    @Column(name = "item_criterion_id")
    private UUID itemCriterionId;

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

    @Column(name = "boolean_value")
    private Boolean booleanValue;

    @Column(name = "number_value")
    private Double numberValue;

    @Column(name = "text_value", columnDefinition = "text")
    private String textValue;

    @Column(columnDefinition = "text")
    private String notes;
}
