package com.backend.backend.dto;


import lombok.Builder;
import java.util.UUID;

@Builder
public record LineCheckItemDto(
        UUID id,                 // LineCheckItemEntity ID
        String itemName,         // from ItemEntity
        String shelfLife,        // from ItemEntity
        String panSize,          // from ItemEntity
        boolean isTool,
        String toolName,// from ItemEntity
        boolean isPortioned,     // from ItemEntity
        String portionSize,      // from ItemEntity
        boolean isTempTaken,     // from ItemEntity
        boolean isCheckMark,     // from ItemEntity
        boolean checked,         // from LineCheckItemEntity
        Double temperature,
        Double minTemp,
        Double maxTemp,// from LineCheckItemEntity
        String notes,            // from LineCheckItemEntity
        String itemNotes         // from ItemEntity
) {}


