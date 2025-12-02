package com.backend.backend.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LineCheckDto {
    private UUID id;
    private UUID userId;
    private String username;
    private LocalDateTime checkTime;
    private LocalDateTime completedAt;

    private List<LineCheckStationDto> stations;
}
