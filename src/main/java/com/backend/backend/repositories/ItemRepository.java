package com.backend.backend.repositories;

import com.backend.backend.entity.ItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<ItemEntity, UUID> {
    Optional<ItemEntity> findByItemName(String itemName);
    List<ItemEntity> findByStationId(UUID id);
    boolean existsByItemName(String itemName);
    boolean existsByItemNameAndStationId(String itemName, UUID stationId);
}




