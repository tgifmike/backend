package com.backend.backend.repositories;

import com.backend.backend.entity.OptionEntity;
import com.backend.backend.entity.StationEntity;
import com.backend.backend.entity.StationHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StationRepository extends JpaRepository<StationEntity, UUID> {
//    Optional<StationEntity> findByStationName(String stationName);
//    boolean existsByStationName(String stationName);
//    boolean existsByStationNameAndLocationId(String stationName, UUID locationId);
//    List<StationEntity> findByLocationId(UUID locationId);
//    Optional<StationEntity> findByIdAndLocationId(UUID id, UUID locationId);
//    boolean existsByStationNameAndLocation_Id(String stationName, UUID locationId);



    // ---------- LOOKUPS ----------
    Optional<StationEntity> findByStationNameIgnoreCase(String stationName);

    Optional<StationEntity> findByIdAndLocation_Id(UUID id, UUID locationId);


    // ---------- EXISTENCE ----------
   // boolean existsByStationNameIgnoreCase(String stationName);

   // boolean existsByStationNameIgnoreCaseAndLocation_Id(String stationName, UUID locationId);

    // ---------- COLLECTIONS ----------
    List<StationEntity> findByLocation_Id(UUID locationId);

    List<StationEntity> findByLocation_IdOrderBySortOrderAsc(UUID locationId);

    List<StationEntity> findByIdOrderBySortOrderAsc(UUID locationId);
}

