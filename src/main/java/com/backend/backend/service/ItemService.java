package com.backend.backend.service;

import com.backend.backend.entity.ItemEntity;

import java.util.UUID;

public interface ItemService {
    ItemEntity createItem(UUID stationId, ItemEntity item);
}
