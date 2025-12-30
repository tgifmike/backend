package com.backend.backend.service;

import com.backend.backend.dto.ItemCreateDto;
import com.backend.backend.dto.ItemUpdateDto;
import com.backend.backend.entity.ItemEntity;

import java.util.List;
import java.util.UUID;

public interface ItemService {
    ItemEntity createItem(ItemCreateDto dto, UUID userId) ;
    List<ItemEntity> getItemsByStation(UUID stationId);
    ItemEntity getItemById(UUID id);
    ItemEntity updateItem(UUID stationId, UUID itemId, ItemUpdateDto dto, UUID userId);
    ItemEntity toggleActive(UUID stationId, UUID itemId, boolean active, UUID userId);
    void deleteItem(UUID id, UUID deletedByUser);
    void reorderItems(UUID stationId, List<UUID> orderedIds, UUID userId);

}
