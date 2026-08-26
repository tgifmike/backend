package com.backend.backend.service;

import com.backend.backend.dto.ItemCriterionDto;
import com.backend.backend.dto.ItemCriterionRequestDto;

import java.util.List;
import java.util.UUID;

public interface ItemCriterionService {

    List<ItemCriterionDto> getByItem(UUID itemId);

    ItemCriterionDto create(UUID itemId, ItemCriterionRequestDto dto);

    ItemCriterionDto update(UUID itemId, UUID criterionId, ItemCriterionRequestDto dto);

    ItemCriterionDto setActive(UUID itemId, UUID criterionId, boolean active);

    void reorder(UUID itemId, List<UUID> orderedCriterionIds);

    void delete(UUID itemId, UUID criterionId);
}
