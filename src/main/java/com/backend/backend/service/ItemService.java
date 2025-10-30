package com.backend.backend.service;

import com.backend.backend.entity.ItemEntity;

import java.util.List;
import java.util.UUID;

public interface ItemService {
    ItemEntity createItem(UUID stationId, ItemEntity item);
    List<ItemEntity> getItemsByStation(UUID stationId);
    ItemEntity getItemById(UUID id);
    ItemEntity updateItem(UUID id, ItemEntity updatedItem);
    ItemEntity toggleActive(UUID id, boolean active);
    void deleteItem(UUID id);

}
