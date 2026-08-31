package com.backend.backend.service;

import com.backend.backend.dto.DashboardMetricsDto;
import com.backend.backend.dto.LineCheckDto;
import com.backend.backend.dto.LineCheckItemCorrectionRequestDto;
import com.backend.backend.dto.LineCheckItemDto;
import com.backend.backend.entity.LineCheckEntity;
import com.backend.backend.entity.UserEntity;

import java.util.List;
import java.util.UUID;

public interface LineCheckService {

    // Create new line check (system generated)
    LineCheckDto createLineCheck(UUID userId, List<UUID> stationIds);

    // Get all line checks as DTO
    List<LineCheckDto> getAllLineChecksDto();
    LineCheckEntity getLineCheckById(UUID id);
    // Get single line check entity by ID
    //LineCheckEntity getLineCheckDtoById(UUID id);
    LineCheckDto getLineCheckDtoById(UUID id);

    // Get single line check as DTO by ID
   // LineCheckDto getLineCheckDtoById(UUID id);
    // LineCheckService.java
    LineCheckDto saveLineCheck(LineCheckDto dto);
    List<LineCheckDto> getCompletedLineChecks();
    // Save/update line check (from mobile app)
    //void updateLineCheck(LineCheckSaveDto dto);
    List<LineCheckDto> getCompletedLineChecksByLocation(UUID locationId);
    DashboardMetricsDto getDashboardMetrics(UUID locationId);

    LineCheckItemDto updateCorrection(
            UUID itemId,
            LineCheckItemCorrectionRequestDto request,
            UserEntity currentUser
    );
}
