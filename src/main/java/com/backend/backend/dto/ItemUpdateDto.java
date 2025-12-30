package com.backend.backend.dto;

import com.backend.backend.enums.ItemTempCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ItemUpdateDto {
    private String itemName;
    private Boolean itemActive;
    private String shelfLife;
    private String panSize;
    private Boolean isTool;
    private String toolName;
    private Boolean isPortioned;
    private String portionSize;
    private Boolean isTempTaken;
    private ItemTempCategory tempCategory;
    private Boolean isCheckMark;
    private String templateNotes;
}

