package com.backend.backend.dto;


import com.backend.backend.enums.ItemType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LineCheckItemDto {

    private UUID id;

    // From ItemEntity
    private String itemName;
    private ItemType itemType;
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

    @JsonProperty("isMissing")
    private Boolean missing;

    private Double temperature;
    private Double minTemp;
    private Double maxTemp;
    private String observations;
    private String templateNotes;
    private Integer sortOrder;
    private List<LineCheckCriterionResponseDto> criterionResponses;

    // Both names are supported for compatibility with existing clients.
    @JsonProperty(value = "isCorrected", access = JsonProperty.Access.READ_ONLY)
    private Boolean isCorrected;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean corrected;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String correctiveNotes;

    // Audit values are set by the dedicated correction endpoint.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Instant correctedAt;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID correctedBy;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String correctedByName;
}
