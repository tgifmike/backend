package com.backend.backend.repositories;

import com.backend.backend.entity.StationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StationRepository extends JpaRepository<StationEntity, UUID> {
    Optional<StationEntity> findByStationName(String stationName);
    boolean existsByStationName(String stationName);
    boolean existsByStationNameAndLocationId(String stationName, UUID locationId);
    List<StationEntity> findByLocationId(UUID locationId);
    Optional<StationEntity> findByIdAndLocationId(UUID id, UUID locationId);
    boolean existsByStationNameAndLocation_Id(String stationName, UUID locationId);

    List<StationEntity> findByLocation_Id(UUID locationId);

    Optional<StationEntity> findByIdAndLocation_Id(UUID id, UUID locationId);
}

