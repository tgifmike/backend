package com.backend.backend.entity;

import com.backend.backend.config.StartOfWeek;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "locations")
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE locations SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class LocationEntity {

    @Id
    @GeneratedValue
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String locationName;

    private String locationStreet;
    private String locationTown;
    private String locationState;
    private String locationZipCode;

    private String locationTimeZone;

    private Double locationLatitude;
    private Double locationLongitude;

    @Enumerated(EnumType.STRING)
    private StartOfWeek startOfWeek = StartOfWeek.MONDAY;

    @Column(nullable = false)
    private Boolean locationActive = true;

    @Column(name = "geocoded_from_zip_fallback")
    private Boolean geocodedFromZipFallback;

    @Column(nullable = false)
    private Integer lineCheckDailyGoal = 1;

    // ---------- RELATIONSHIP ----------

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @JsonBackReference("acc")
    private AccountEntity account;

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
