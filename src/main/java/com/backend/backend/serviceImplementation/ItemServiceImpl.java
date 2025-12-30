package com.backend.backend.serviceImplementation;

import com.backend.backend.dto.ItemCreateDto;
import com.backend.backend.dto.ItemUpdateDto;
import com.backend.backend.entity.*;
import com.backend.backend.enums.HistoryType;
import com.backend.backend.enums.OptionType;
import com.backend.backend.repositories.*;
import com.backend.backend.service.ItemService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.type.descriptor.java.ObjectJavaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final StationRepository stationRepository;
    private final ItemHistoryRepository itemRepositoryHistory;
    private final UserRepository userRepository;

    //---------Helper-------------------
    private void recordHistory(
            ItemEntity item,
            UUID changedBy,
            HistoryType changeType,
            Map<String, Object> oldValues
    ){
        String changedByName = getUserNameById(changedBy);

        ItemHistoryEntity history = ItemHistoryEntity.builder()
                .itemId(item.getId())
                .station(item.getStation())
                .itemName(item.getItemName())
                .sortOrder(item.getSortOrder())
                .itemActive(item.getItemActive())
                .shelfLife(item.getShelfLife())
                .panSize(item.getPanSize())
                .toolName(item.getToolName())
                .isTool(item.getIsTool())
                .portionSize(item.getPortionSize())
                .isPortioned(item.getIsPortioned())
                .isTempTaken(item.getIsTempTaken())
                .tempCategory(item.getTempCategory())
                .isCheckMark(item.getIsCheckMark())
                .templateNotes(item.getTemplateNotes())
                .changeType(changeType)
                .changedBy(changedBy)
                .changedByName(changedByName)
                .changeAt(Instant.now())
                .oldValues(oldValues != null
                        ? oldValues.entrySet().stream()
                        .filter(e -> e.getKey() != null && e.getValue() != null) // skip null keys & values
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().toString(),
                                (existing, replacement) -> existing
                        ))
                        : new HashMap<>()
                )


                .build();

        itemRepositoryHistory.save(history);
}

    private String getUserNameById(UUID userId) {
        if (userId == null) return "System";
        return userRepository.findById(userId)
                .map(u -> u.getUserName() != null ? u.getUserName() : u.getId().toString())
                .orElse("Unknown User");
    }

    // ------------------- GETTERS -------------------

    // get all items for station
    @Override
    @Transactional
    public List<ItemEntity> getItemsByStation(UUID stationId){
        return itemRepository.findByStationIdAndDeletedAtIsNull(stationId);
    }


    //get item by id
    @Override
    @Transactional
    public ItemEntity getItemById(UUID id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Item not found with ID: " + id));
    }

    // ------------------- CREATE -------------------
    @Override
    public ItemEntity createItem(ItemCreateDto dto, UUID userId) {
        // fetch station
        StationEntity station = stationRepository.findById(dto.getStationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Station not found"));

        // Determine sortOrder
        int sortOrder = dto.getSortOrder() != null ? dto.getSortOrder() : getNextSortOrder(station.getId());

        ItemEntity item = ItemEntity.builder()
                .itemName(dto.getItemName())
                .itemActive(dto.getItemActive())
                .shelfLife(dto.getShelfLife())
                .panSize(dto.getPanSize())
                .isTool(dto.getIsTool())
                .toolName(dto.getToolName())
                .isPortioned(dto.getIsPortioned())
                .portionSize(dto.getPortionSize())
                .isTempTaken(dto.getIsTempTaken())
                .tempCategory(dto.getTemperatureCategory())
                .isCheckMark(dto.getIsCheckMark())
                .templateNotes(dto.getTemplateNotes())
                .station(station)
                .sortOrder(sortOrder) // <- set here
                .createdBy(userId)
                .build();

        ItemEntity saved = itemRepository.save(item);

        // Record history
        recordHistory(saved, userId, HistoryType.CREATED, null);

        return saved;
    }

    // Helper to get next available sortOrder for a station
    private int getNextSortOrder(UUID stationId) {
        return itemRepository.findByStationIdAndDeletedAtIsNull(stationId)
                .stream()
                .map(ItemEntity::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compare)
                .orElse(-1) + 1; // start from 0
    }


    @Override
    public ItemEntity updateItem(UUID stationId, UUID itemId, ItemUpdateDto dto, UUID userId) {
        ItemEntity existing = itemRepository.findById(itemId)
                .orElseThrow(() -> new NoSuchElementException("Item not found"));

        Map<String, Object> oldValues = new HashMap<>();

        // Compare and update fields
        if (dto.getItemName() != null && !dto.getItemName().equals(existing.getItemName())) {
            oldValues.put("itemName", existing.getItemName());
            existing.setItemName(dto.getItemName());
        }

        if (dto.getItemActive() != null && !dto.getItemActive().equals(existing.getItemActive())) {
            oldValues.put("itemActive", existing.getItemActive());
            existing.setItemActive(dto.getItemActive());
        }

        if (dto.getShelfLife() != null && !dto.getShelfLife().equals(existing.getShelfLife())) {
            oldValues.put("shelfLife", existing.getShelfLife());
            existing.setShelfLife(dto.getShelfLife());
        }

        if (dto.getPanSize() != null && !dto.getPanSize().equals(existing.getPanSize())) {
            oldValues.put("panSize", existing.getPanSize());
            existing.setPanSize(dto.getPanSize());
        }

        if (dto.getIsTool() != null && !dto.getIsTool().equals(existing.getIsTool())) {
            oldValues.put("isTool", existing.getIsTool());
            existing.setIsTool(dto.getIsTool());
        }

        if (dto.getToolName() != null && !dto.getToolName().equals(existing.getToolName())) {
            oldValues.put("toolName", existing.getToolName());
            existing.setToolName(dto.getToolName());
        }

        if (dto.getIsPortioned() != null && !dto.getIsPortioned().equals(existing.getIsPortioned())) {
            oldValues.put("isPortioned", existing.getIsPortioned());
            existing.setIsPortioned(dto.getIsPortioned());
        }

        if (dto.getPortionSize() != null && !dto.getPortionSize().equals(existing.getPortionSize())) {
            oldValues.put("portionSize", existing.getPortionSize());
            existing.setPortionSize(dto.getPortionSize());
        }

        if (dto.getIsTempTaken() != null && !dto.getIsTempTaken().equals(existing.getIsTempTaken())) {
            oldValues.put("isTempTaken", existing.getIsTempTaken());
            existing.setIsTempTaken(dto.getIsTempTaken());
        }

        if (dto.getTempCategory() != null && !dto.getTempCategory().equals(existing.getTempCategory())) {
            oldValues.put("tempCategory", existing.getTempCategory());
            existing.setTempCategory(dto.getTempCategory());
        }

        if (dto.getIsCheckMark() != null && !dto.getIsCheckMark().equals(existing.getIsCheckMark())) {
            oldValues.put("isCheckMark", existing.getIsCheckMark());
            existing.setIsTempTaken(dto.getIsCheckMark());
        }

        if (dto.getTemplateNotes() != null && !dto.getTemplateNotes().equals(existing.getTemplateNotes())) {
            oldValues.put("templateNotes", existing.getTemplateNotes());
            existing.setTemplateNotes(dto.getTemplateNotes());
        }

        // Audit info
        existing.setUpdatedBy(userId);
        existing.setUpdatedAt(Instant.now());

        // Save
        ItemEntity saved = itemRepository.save(existing);

        // Record history if any changes
        if (!oldValues.isEmpty()) {
            recordHistory(saved, userId, HistoryType.UPDATED, oldValues);
        }

        return saved;
    }




    // ------------------- DELETE -------------------
    @Override
    @Transactional
    public void deleteItem(UUID itemId, UUID deletedByUser) {
        ItemEntity item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NoSuchElementException("Item not found"));

        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("itemName", item.getItemName());
        oldValues.put("sortOrder", item.getSortOrder());
        oldValues.put("itemActive", item.getItemActive());
        oldValues.put("shelfLife", item.getShelfLife());
        oldValues.put("panSize", item.getPanSize());
        oldValues.put("isTool", item.getIsTool());
        oldValues.put("toolName", item.getToolName());
        oldValues.put("isPortioned", item.getIsPortioned());
        oldValues.put("portionSize", item.getPortionSize());
        oldValues.put("isTempTaken", item.getIsTempTaken());
        oldValues.put("tempCategory", item.getTempCategory());
        oldValues.put("isCheckMark", item.getIsCheckMark());
        oldValues.put("templateNotes", item.getTemplateNotes());

        item.setDeletedAt(Instant.now());
        item.setDeletedBy(deletedByUser);
        item.setUpdatedAt(Instant.now());


        itemRepository.saveAndFlush(item);

        recordHistory(item, deletedByUser, HistoryType.DELETED, oldValues);
    }


    // ------------------- TOGGLE ACTIVE -------------------
    @Override
    @Transactional
    public ItemEntity toggleActive(UUID stationId, UUID itemId, boolean active, UUID userId) {

        ItemEntity item = itemRepository.findById(itemId)
                .orElseThrow(()-> new NoSuchElementException("Item not found"));


        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("itemActive", item.getItemActive());

        item.setItemActive(active);
        item.setUpdatedBy(userId);
        item.setUpdatedAt(Instant.now());

        ItemEntity saved = itemRepository.save(item);

        recordHistory(saved, userId, HistoryType.UPDATED, oldValues);

        return saved;
    }

    // ------------------- REORDER -------------------
    @Override
    @Transactional
    public void reorderItems(UUID stationId, List<UUID> orderedIds, UUID userId) {
        List<ItemEntity> items = itemRepository
                .findAllByStationIdAndDeletedAtIsNullOrderBySortOrderAsc(stationId);

        Map<UUID, ItemEntity> itemMap = items.stream()
                .collect(Collectors.toMap(ItemEntity::getId, item -> item));

        for (int i = 0; i < orderedIds.size(); i++) {
            UUID id = orderedIds.get(i);
            ItemEntity item = itemMap.get(id);
            if (item != null) {
                Integer oldSort = item.getSortOrder(); // capture old value before updating

                if (!Objects.equals(oldSort, i)) {
                    item.setSortOrder(i);
                    item.setUpdatedAt(Instant.now());

                    recordHistory(item, userId, HistoryType.UPDATED,
                            Map.of("sortOrder", oldSort)); // pass old value
                }
            }
        }


        itemRepository.saveAll(items);
        itemRepository.flush(); // force DB update
    }






}
