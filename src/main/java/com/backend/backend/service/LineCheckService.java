package com.backend.backend.service;

import com.backend.backend.dto.LineCheckDto;
import com.backend.backend.entity.LineCheckEntity;
import com.backend.backend.entity.StationEntity;
import com.backend.backend.entity.UserEntity;

import java.util.List;
import java.util.UUID;

public interface LineCheckService {
    LineCheckEntity createLineCheck(UserEntity user, List<StationEntity> stations);
    List<LineCheckDto> getAllLineChecksDto();
    public LineCheckEntity getLineCheckById(UUID id);
    LineCheckEntity saveLineCheck(LineCheckDto lineCheckDto);

}
