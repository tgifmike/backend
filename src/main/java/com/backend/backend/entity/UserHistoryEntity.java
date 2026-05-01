package com.backend.backend.entity;


import com.backend.backend.enums.HistoryType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "user_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserHistoryEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    private String userName;
    private String userEmail;
    private Boolean userActive;
    private String accessRole;
    private String appRole;

    private Instant changeAt;

    @Column(nullable = false)
    private UUID changedBy;

    @Column(nullable = false)
    private String changedByName;

    @Enumerated(EnumType.STRING)
    private HistoryType changeType;

    @ElementCollection
    @CollectionTable(
            name = "user_history_old_values",
            joinColumns = @JoinColumn(name = "history_id")
    )
    @MapKeyColumn(name = "field_name")
    @Column(name = "old_value")
    private Map<String,String> oldValues = new HashMap<>();
}