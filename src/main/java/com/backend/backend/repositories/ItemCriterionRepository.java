package com.backend.backend.repositories;

import com.backend.backend.entity.ItemCriterionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ItemCriterionRepository extends JpaRepository<ItemCriterionEntity, UUID> {

    List<ItemCriterionEntity> findByItem_IdOrderBySortOrderAscLabelAsc(UUID itemId);
}
