package com.backend.backend.entity;

import com.backend.backend.enums.OptionType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "options")
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE options SET deleted_at = CURRENT_TIMESTAMP, deleted_by = ? WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class OptionEntity {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "option_name", nullable = false)
    private String optionName;

    @Column(name = "option_active", nullable = false)
    private boolean optionActive;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_type")
    private OptionType optionType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private AccountEntity account;

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

