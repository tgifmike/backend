package com.backend.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemAuditDto {
    private UUID id;               // unique ID for this history record
    private String itemName;     // option name
    private String fieldName;      // which field changed: "optionName", "optionActive", etc.
    private String oldValue;       // old value as string
    private String newValue;       // new value as string
    private String changeType;     // "CREATED", "UPDATED", "DELETED"
    private UUID changedBy;
    private String changedByName;// user who made the change
    private Instant changeAt;
}
