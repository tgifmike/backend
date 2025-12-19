package com.backend.backend.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LineCheckItemDto {

    private UUID id;

    // From ItemEntity
    private String itemName;
    private String shelfLife;
    private String panSize;
    private boolean tool;
    private String toolName;
    private boolean portioned;
    private String portionSize;
    private boolean tempTaken;
    private boolean checkMark;

    // From LineCheckItemEntity
    @JsonProperty("itemChecked") // <-- force Jackson to map JSON "itemChecked"
    private boolean itemChecked;

    private Double temperature;
    private Double minTemp;
    private Double maxTemp;
    private String observations;
    private String templateNotes;
    private Integer sortOrder;
}
