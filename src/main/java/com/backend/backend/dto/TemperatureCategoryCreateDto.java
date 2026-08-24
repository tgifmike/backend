package com.backend.backend.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TemperatureCategoryCreateDto {

    @NotNull
    private UUID locationId;

    @NotBlank
    private String code;

    @NotBlank
    private String name;

    @NotNull
    private Double minTemp;

    @NotNull
    private Double maxTemp;

    private String unit = "F";
    private Boolean active = true;
    private Integer sortOrder;

    @AssertTrue(message = "minTemp must be less than maxTemp")
    public boolean isTemperatureRangeValid() {
        return minTemp == null || maxTemp == null || minTemp < maxTemp;
    }
}
