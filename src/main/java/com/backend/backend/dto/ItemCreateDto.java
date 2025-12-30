package com.backend.backend.dto;

import com.backend.backend.enums.ItemTempCategory;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ItemCreateDto {

    @NotNull
    private String ItemName;

    private Boolean itemActive = true;
    private String shelfLife;
    private String panSize;
    private Integer sortOrder;

    private String toolName;
    private Boolean isTool;

    private String portionSize;
    private Boolean isPortioned;

    private ItemTempCategory temperatureCategory;
    private Boolean isTempTaken;

    private Boolean isCheckMark;

    private String templateNotes;

    @NotNull
    private UUID stationId;
}
