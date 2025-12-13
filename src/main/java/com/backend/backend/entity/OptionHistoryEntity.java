package com.backend.backend.entity;


import com.backend.backend.enums.OptionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "option_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptionHistoryEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID optionId;

    @Column(nullable = false)
    private UUID accountId;

    private String optionName;
    private Boolean optionActive;

    @Enumerated(EnumType.STRING)
    private OptionType optionType;

    private Integer sortOrder;

    private Instant changeAt;

    @Column(nullable = false)
    private UUID changedBy;

    @Column(nullable = false)
    private String changedByName; // store the user name at the time of change

    @Enumerated(EnumType.STRING)
    private ChangeType changeType;


    // 📝 Store old values
    @ElementCollection
    @CollectionTable(
            name = "option_history_old_values",
            joinColumns = @JoinColumn(name = "history_id")
    )
    @MapKeyColumn(name = "field_name")
    @Column(name = "old_value")
    private Map<String, String> oldValues = new HashMap<>();

    public enum ChangeType {
        CREATED,
        UPDATED,
        DELETED
    }
}
