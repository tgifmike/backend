package com.backend.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardMetricsDto {

    private long totalChecksToday;
    private long totalChecksWeekToDate;

    private long missingItemsToday;
    private long outOfTempItemsToday;
    private List<String> outOfTempItemNamesToday;

    private long incorrectPrepItemsToday;
    private List<String> incorrectPrepItemNamesToday;

    private Long durationSeconds;

}
