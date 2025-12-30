package com.backend.backend.repositories;

import com.backend.backend.entity.ItemHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ItemHistoryRepository extends JpaRepository<ItemHistoryEntity, UUID> {

    List<ItemHistoryEntity> findByStationIdOrderByChangeAtDesc(UUID stationId);

    List<ItemHistoryEntity> findByItemIdOrderByChangeAtDesc(UUID itemId);
}
