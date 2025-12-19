package com.backend.backend.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class StationDto {
    private UUID id;
    private String stationName;
    private Boolean stationActive;
    private Integer sortOrder;
    private List<LineCheckItemDto> items;

}
