package com.backend.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardMetricsDto {

    // Line check totals
    private long totalChecksToday;
    private long totalChecksYesterday;
    private long totalChecksWeekToDate;
    private long totalChecksMonthToDate;

    // Employee productivity metrics
    private List<EmployeeCheckCountDto> employeeChecksToday;
    private List<EmployeeCheckCountDto> employeeChecksWeek;
    private List<EmployeeCheckCountDto> employeeChecksMonth;

    // Employee performance metrics (average duration, etc.)
    private List<EmployeePerformanceDto> employeePerformanceToday;

    // Issue summary totals (today)
    private long missingItemsToday;
    private List<String> missingItemNamesToday;
    private long outOfTempItemsToday;
    private List<String> outOfTempItemNamesToday;
    private long incorrectPrepItemsToday;
    private List<String> incorrectPrepItemNamesToday;

    // Average completion duration (today)
    private long durationSeconds;

    // Detailed issue breakdown
    private List<LineCheckItemIssuesDto> lineChecks;
}
