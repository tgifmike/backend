package com.backend.backend.repositories;

import com.backend.backend.entity.StationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StationRepository extends JpaRepository<StationEntity, UUID> {
    Optional<StationEntity> findByStationName(String stationName);
    boolean existsByStationName(String stationName);
}

