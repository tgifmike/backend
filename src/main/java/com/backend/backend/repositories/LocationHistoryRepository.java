package com.backend.backend.repositories;

import com.backend.backend.entity.LocationHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LocationHistoryRepository extends JpaRepository<LocationHistoryEntity, UUID> {

    // Query by Location ID
    List<LocationHistoryEntity> findByLocation_IdOrderByChangeAtDesc(UUID locationId);

    // Query by Account ID (via location)
    List<LocationHistoryEntity> findByLocation_Account_IdOrderByChangeAtDesc(UUID accountId);

//    List<LocationHistoryEntity> findByLocation_Account_IdOrderByChangeAtDesc(UUID accountId);
//
//    List<LocationHistoryEntity> findAllByOrderByChangeAtDesc();

    List<LocationHistoryEntity> findAllByOrderByChangeAtDesc();
}


