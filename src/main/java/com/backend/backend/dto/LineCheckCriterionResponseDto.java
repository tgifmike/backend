package com.backend.backend.dto;

import com.backend.backend.enums.ResponseType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LineCheckCriterionResponseDto {

    private UUID id;
    private UUID itemCriterionId;
    private String label;
    private ResponseType responseType;
    private Boolean required;
    private Boolean requireNotesOnFailure;
    private Double minValue;
    private Double maxValue;
    private String unit;
    private Integer sortOrder;
    private Boolean booleanValue;
    private Double numberValue;
    private String textValue;
    private String notes;
    private Boolean failed;
    private List<UUID> photoIds = new ArrayList<>();
}
