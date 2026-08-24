package com.backend.backend.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record TemperatureCategoryDto(
        UUID id,
        UUID locationId,
        String code,
        String name,
        Double minTemp,
        Double maxTemp,
        String unit,
        Boolean active,
        Boolean systemDefault,
        Integer sortOrder
) {
}
