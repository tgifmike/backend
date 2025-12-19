package com.backend.backend.repositories;


import com.backend.backend.entity.LineCheckItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LineCheckItemRepository extends JpaRepository<LineCheckItemEntity, UUID> {
    List<LineCheckItemEntity> findByLineCheckStation_Id(UUID stationId);
}

