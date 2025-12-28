package com.backend.backend.entity;


import com.backend.backend.enums.HistoryType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "account_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountHistoryEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID accountId;

    private String accountName;
    private Boolean accountActive;
    private String imageBase64;

    private Instant changeAt;

    @Column(nullable = false)
    private UUID changedBy;

    @Column(nullable = false)
    private String changedByName;

    @Enumerated(EnumType.STRING)
    private HistoryType changeType;

    @ElementCollection
    @CollectionTable(
            name = "account_history_old_values",
            joinColumns = @JoinColumn(name = "history_id")
    )
    @MapKeyColumn(name = "field_name")
    @Column(name = "old_value")
    private Map<String, String> oldValues = new HashMap<>();


}

