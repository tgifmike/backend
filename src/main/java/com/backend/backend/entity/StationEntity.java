package com.backend.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name= "stations")
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE stations SET deleted_at = CURRENT_TIMESTAMP, deleted_by = ? WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StationEntity {

        @Id
        @GeneratedValue
        @Column(name = "id", updatable = false, nullable = false)
        private UUID id;

        private String stationName;
        private boolean stationActive = true;

        @Column(name = "sort_order")
        private Integer sortOrder;

        @ManyToOne
        @JoinColumn(name = "location_id")
        private LocationEntity location;

        // ---------- RELATIONSHIP ----------

        //need to add when i add items
        @OneToMany(mappedBy = "station", cascade = CascadeType.ALL, orphanRemoval = true)
        @JsonManagedReference("station-items")
        private List<ItemEntity> items = new ArrayList<>();

        @OneToMany(mappedBy = "station", fetch = FetchType.LAZY)
        @JsonManagedReference("station-linechecks")
        private List<LineCheckStationEntity> lineCheckStations = new ArrayList<>();



// ---------- AUDITING ----------

        @CreatedDate
        @Column(updatable = false)
        private Instant createdAt;

        @LastModifiedDate
        private Instant updatedAt;

        @CreatedBy
        @Column(updatable = false)
        private UUID createdBy;

        @LastModifiedBy
        private UUID updatedBy;

        // ---------- SOFT DELETE ----------

        @Column
        private Instant deletedAt;

        @Column
        private UUID deletedBy;
}

