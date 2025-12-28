package com.backend.backend.repositories;

import com.backend.backend.entity.StationHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StationHistoryRepository extends JpaRepository<StationHistoryEntity, UUID> {

    List<StationHistoryEntity> findByStationIdOrderByChangeAtDesc(UUID stationId);

    //for history
    List<StationHistoryEntity> findByStationId(UUID stationId);


    //for history
    List<StationHistoryEntity> findByStation_Location_Id(UUID locationId);
    List<StationHistoryEntity>
    findByStation_Location_IdOrderByChangeAtDesc(UUID locationId);

    // fetch all history for a location regardless of soft-deleted stations
    @Query("SELECT h FROM StationHistoryEntity h WHERE h.locationId = :locationId ORDER BY h.changeAt DESC")
    List<StationHistoryEntity> findAllByLocationId(@Param("locationId") UUID locationId);


}
