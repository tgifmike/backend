package com.backend.backend.dto;

import com.backend.backend.enums.ResponseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemCriterionRequestDto {

    @NotBlank
    private String label;

    @NotNull
    private ResponseType responseType;

    private Boolean required = false;
    private Boolean requireNotesOnFailure = false;
    private Double minValue;
    private Double maxValue;
    private String unit;
    private Integer sortOrder;
    private Boolean active = true;
}
