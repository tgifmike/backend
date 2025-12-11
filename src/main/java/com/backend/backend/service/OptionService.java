package com.backend.backend.service;

import com.backend.backend.config.OptionType;
import com.backend.backend.dto.OptionCreateDto;
import com.backend.backend.entity.OptionEntity;

import java.util.List;
import java.util.UUID;

public interface OptionService {

    List<OptionEntity> getAllOptions(UUID accountId);

    List<OptionEntity> getOptionsByType(UUID accountId, OptionType optionType);

    OptionEntity createOption(OptionCreateDto dto, UUID userId);

    OptionEntity updateOption(UUID optionId, OptionEntity option);

    void deleteOption(UUID optionId);

    void reorderOptions(UUID accountId, OptionType optionType, List<UUID> orderedIds);

    List<OptionEntity> getOptionsByAccount(UUID accountId);

    OptionEntity toggleActive(UUID id, boolean active, UUID userId);

}

