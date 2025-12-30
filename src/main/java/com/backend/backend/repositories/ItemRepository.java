package com.backend.backend.repositories;

import com.backend.backend.entity.ItemEntity;
import com.backend.backend.entity.OptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<ItemEntity, UUID> {
    //Optional<ItemEntity> findByItemName(String itemName);
    List<ItemEntity> findByStationId(UUID id);
    //boolean existsByItemName(String itemName);
    boolean existsByItemNameAndStationId(String itemName, UUID stationId);
    Optional<ItemEntity> findByItemNameAndStationId(String itemName, UUID stationId);
    List<ItemEntity> findAllByStationIdOrderBySortOrderAsc(UUID stationId);

    @Query(
            value = "SELECT * FROM items WHERE station_id = :stationId ORDER BY created_at ASC",
            nativeQuery = true
    )
    List<ItemEntity> findAllByAccountIdIncludingDeleted(UUID stationId);

    // Only non-deleted items for a station
    List<ItemEntity> findByStationIdAndDeletedAtIsNull(UUID stationId);

    List<ItemEntity> findAllByStationIdAndDeletedAtIsNullOrderBySortOrderAsc(UUID stationId);
}




