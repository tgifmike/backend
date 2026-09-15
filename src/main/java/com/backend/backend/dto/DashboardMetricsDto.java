package com.backend.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardMetricsDto {

    private LocalDate operationalDate;
    private String timeZone;
    private LocalTime endOfDay;
    private String startOfWeek;
    private int daysElapsedWeek;
    private int daysElapsedMonth;

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

    //most days misseded
    private String mostMissingItemsDay;
    private String mostOutOfTempDay;
    private String mostIncorrectPrepDay;
    private String weakestLineCheckDay;

    //upgrate most days missed etc
    private RankedDayDto[] topMissingDays;
    private RankedDayDto[] topOutOfTempDays;
    private RankedDayDto[] topIncorrectPrepDays;
    private RankedDayDto[] topWeakestCompletionDays;

    private RankedItemDto[] topMissingItems;
    private RankedItemDto[] topOutOfTempItems;
    private RankedItemDto[] topIncorrectPrepItems;

    // Detailed issue breakdown
    private List<LineCheckItemIssuesDto> lineChecks;
}
