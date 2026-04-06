package com.backend.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeePerformanceDto {

    private UUID userId;
    private String userName;
    private Long checkCount;
    private Double avgCompletionSeconds;
}
