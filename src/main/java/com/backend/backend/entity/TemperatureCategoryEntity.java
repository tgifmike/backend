package com.backend.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Locale;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "temperature_categories",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_temperature_categories_location_code",
                columnNames = {"location_id", "code"}
        )
)
public class TemperatureCategoryEntity {

    @Id
    @GeneratedValue
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    @JsonIgnore
    private LocationEntity location;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "min_temp", nullable = false)
    private Double minTemp;

    @Column(name = "max_temp", nullable = false)
    private Double maxTemp;

    @Builder.Default
    @Column(nullable = false, length = 10)
    private String unit = "F";

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Column(name = "system_default", nullable = false)
    private Boolean systemDefault = false;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @PrePersist
    @PreUpdate
    void normalizeAndValidate() {
        if (code != null) {
            code = code.trim().toUpperCase(Locale.ROOT);
        }
        if (name != null) {
            name = name.trim();
        }
        if (unit == null || unit.isBlank()) {
            unit = "F";
        } else {
            unit = unit.trim().toUpperCase(Locale.ROOT);
        }
        if (minTemp == null || maxTemp == null || minTemp >= maxTemp) {
            throw new IllegalArgumentException("minTemp must be less than maxTemp");
        }
    }
}
