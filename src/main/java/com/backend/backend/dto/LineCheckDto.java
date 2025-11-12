package com.backend.backend.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Builder
public record LineCheckDto(
        UUID id,
        String username,
        LocalDateTime checkTime,
        List<LineCheckStationDto> stations
) {}
