package com.backend.backend.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LineCheckItemDto {
    UUID id;                 // LineCheckItemEntity ID
    String itemName;        // from ItemEntity
    String shelfLife;        // from ItemEntity
    String panSize;        // from ItemEntity
    boolean isTool;
    String toolName;// from ItemEntity
    boolean isPortioned;     // from ItemEntity
    String portionSize;      // from ItemEntity
    boolean isTempTaken;     // from ItemEntity
    boolean isCheckMark;     // from ItemEntity
    boolean isItemChecked;         // from LineCheckItemEntity
    Double temperature;
    Double minTemp;
    Double maxTemp;// from LineCheckItemEntity
    String observations;
    String templateNotes;
    Integer sortOrder;
    }


