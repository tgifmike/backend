package com.backend.backend.serviceImplementation;

import com.backend.backend.dto.ItemCriterionDto;
import com.backend.backend.dto.ItemCriterionRequestDto;
import com.backend.backend.entity.ItemCriterionEntity;
import com.backend.backend.entity.ItemEntity;
import com.backend.backend.enums.ResponseType;
import com.backend.backend.repositories.ItemCriterionRepository;
import com.backend.backend.repositories.ItemRepository;
import com.backend.backend.service.ItemCriterionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ItemCriterionServiceImpl implements ItemCriterionService {

    private final ItemCriterionRepository itemCriterionRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ItemCriterionDto> getByItem(UUID itemId) {
        requireItem(itemId);
        return itemCriterionRepository
                .findByItem_IdOrderBySortOrderAscLabelAsc(itemId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ItemCriterionDto create(UUID itemId, ItemCriterionRequestDto dto) {
        ItemEntity item = requireItem(itemId);
        validate(dto);

        Integer sortOrder = dto.getSortOrder();
        if (sortOrder == null) {
            sortOrder = itemCriterionRepository
                    .findByItem_IdOrderBySortOrderAscLabelAsc(itemId)
                    .stream()
                    .map(ItemCriterionEntity::getSortOrder)
                    .filter(java.util.Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(-1) + 1;
        }

        ItemCriterionEntity criterion = ItemCriterionEntity.builder()
                .item(item)
                .label(dto.getLabel().trim())
                .responseType(dto.getResponseType())
                .required(Boolean.TRUE.equals(dto.getRequired()))
                .requireNotesOnFailure(Boolean.TRUE.equals(dto.getRequireNotesOnFailure()))
                .minValue(dto.getMinValue())
                .maxValue(dto.getMaxValue())
                .unit(normalizeUnit(dto.getUnit()))
                .sortOrder(sortOrder)
                .active(dto.getActive() == null || dto.getActive())
                .build();

        return toDto(itemCriterionRepository.save(criterion));
    }

    @Override
    @Transactional
    public ItemCriterionDto update(
            UUID itemId,
            UUID criterionId,
            ItemCriterionRequestDto dto
    ) {
        validate(dto);
        ItemCriterionEntity criterion = requireCriterion(itemId, criterionId);

        criterion.setLabel(dto.getLabel().trim());
        criterion.setResponseType(dto.getResponseType());
        criterion.setRequired(Boolean.TRUE.equals(dto.getRequired()));
        criterion.setRequireNotesOnFailure(
                Boolean.TRUE.equals(dto.getRequireNotesOnFailure())
        );
        criterion.setMinValue(dto.getMinValue());
        criterion.setMaxValue(dto.getMaxValue());
        criterion.setUnit(normalizeUnit(dto.getUnit()));
        criterion.setSortOrder(dto.getSortOrder());
        criterion.setActive(dto.getActive() == null || dto.getActive());

        return toDto(itemCriterionRepository.save(criterion));
    }

    @Override
    @Transactional
    public ItemCriterionDto setActive(UUID itemId, UUID criterionId, boolean active) {
        ItemCriterionEntity criterion = requireCriterion(itemId, criterionId);
        criterion.setActive(active);
        return toDto(itemCriterionRepository.save(criterion));
    }

    @Override
    @Transactional
    public void reorder(UUID itemId, List<UUID> orderedCriterionIds) {
        requireItem(itemId);

        if (orderedCriterionIds == null) {
            throw badRequest("orderedCriterionIds is required");
        }

        Set<UUID> uniqueIds = new HashSet<>(orderedCriterionIds);
        if (uniqueIds.size() != orderedCriterionIds.size()) {
            throw badRequest("Criterion order contains duplicate IDs");
        }

        List<ItemCriterionEntity> criteria = itemCriterionRepository
                .findByItem_IdOrderBySortOrderAscLabelAsc(itemId);
        Map<UUID, ItemCriterionEntity> byId = new HashMap<>();
        for (ItemCriterionEntity criterion : criteria) {
            byId.put(criterion.getId(), criterion);
        }

        if (!byId.keySet().equals(uniqueIds)) {
            throw badRequest("Criterion order must contain every criterion for the item exactly once");
        }

        for (int index = 0; index < orderedCriterionIds.size(); index++) {
            byId.get(orderedCriterionIds.get(index)).setSortOrder(index);
        }

        itemCriterionRepository.saveAll(criteria);
    }

    @Override
    @Transactional
    public void delete(UUID itemId, UUID criterionId) {
        itemCriterionRepository.delete(requireCriterion(itemId, criterionId));
    }

    private ItemEntity requireItem(UUID itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Item not found"
                ));
    }

    private ItemCriterionEntity requireCriterion(UUID itemId, UUID criterionId) {
        ItemCriterionEntity criterion = itemCriterionRepository.findById(criterionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Item criterion not found"
                ));

        if (!criterion.getItem().getId().equals(itemId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item criterion not found");
        }

        return criterion;
    }

    private void validate(ItemCriterionRequestDto dto) {
        boolean numeric = dto.getResponseType() == ResponseType.TEMPERATURE
                || dto.getResponseType() == ResponseType.NUMBER;

        if (!numeric && (dto.getMinValue() != null || dto.getMaxValue() != null)) {
            throw badRequest("minValue and maxValue are only valid for numeric criteria");
        }

        if (!numeric && dto.getUnit() != null && !dto.getUnit().isBlank()) {
            throw badRequest("unit is only valid for numeric criteria");
        }

        if (dto.getMinValue() != null
                && dto.getMaxValue() != null
                && dto.getMinValue() > dto.getMaxValue()) {
            throw badRequest("minValue must be less than or equal to maxValue");
        }
    }

    private String normalizeUnit(String unit) {
        return unit == null || unit.isBlank() ? null : unit.trim();
    }

    private ItemCriterionDto toDto(ItemCriterionEntity criterion) {
        return new ItemCriterionDto(
                criterion.getId(),
                criterion.getItem().getId(),
                criterion.getLabel(),
                criterion.getResponseType(),
                criterion.getRequired(),
                criterion.getRequireNotesOnFailure(),
                criterion.getMinValue(),
                criterion.getMaxValue(),
                criterion.getUnit(),
                criterion.getSortOrder(),
                criterion.getActive()
        );
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
