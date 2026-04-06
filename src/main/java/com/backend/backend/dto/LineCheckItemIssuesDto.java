package com.backend.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LineCheckItemIssuesDto {

    private UUID lineCheckId;
    private Instant checkTime;

    private long missingCount;
    private List<String> missingItems;

    private long outOfTempCount;
    private List<String> outOfTempItems;

    private long incorrectPrepCount;
    private List<String> incorrectPrepItems;

    private long totalChecksMonthToDate;

    private long totalChecksYesterday;

    private String employeeName;

}