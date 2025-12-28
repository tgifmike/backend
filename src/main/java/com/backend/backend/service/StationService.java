package com.backend.backend.service;

import com.backend.backend.dto.StationDto;
import com.backend.backend.entity.StationEntity;
import com.backend.backend.entity.UserEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface StationService {

    // ---------------- CREATE ----------------
    StationEntity createStation(UUID locationId, StationEntity station, UUID userId);

    // ---------------- UPDATE (FULL) ----------------
    StationEntity updateStation(UUID stationId, Map<String, Object> updates, UUID userId);

    // ---------------- PARTIAL UPDATE ----------------
//    StationEntity partialUpdate(
//            UUID locationId,
//            UUID stationId,
//            Map<String, Object> updates,
//            UserEntity user
//    );

    // ---------------- TOGGLE ACTIVE ----------------
    StationEntity toggleActive(UUID stationId, boolean active, UUID userId);

    // ---------------- DELETE ----------------
    void deleteStation(
            UUID stationId,
            UUID userId
    );

    //reorder
    void reorderStations(
            UUID locationId,
            List<UUID> orderedIds,
            UUID userId
    );


    // ---------------- READ ----------------
    StationEntity getStationById(UUID id);

    StationEntity getStationByName(String stationName);

    List<StationEntity> getAllStations();

    List<StationDto> getStationsByLocation(UUID locationId);
}


