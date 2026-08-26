package com.backend.backend.dto;

import com.backend.backend.enums.ResponseType;

import java.util.UUID;

public record ItemCriterionDto(
        UUID id,
        UUID itemId,
        String label,
        ResponseType responseType,
        Boolean required,
        Boolean requireNotesOnFailure,
        Double minValue,
        Double maxValue,
        String unit,
        Integer sortOrder,
        Boolean active
) {
}
