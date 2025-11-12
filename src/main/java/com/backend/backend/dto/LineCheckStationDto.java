package com.backend.backend.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record LineCheckStationDto(
        UUID id,
        String stationName,
        List<LineCheckItemDto> items
) {}
