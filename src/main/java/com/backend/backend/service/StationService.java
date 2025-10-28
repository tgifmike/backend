package com.backend.backend.service;

import com.backend.backend.entity.StationEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface StationService {
    StationEntity createStation(UUID locationId, StationEntity station);
    void deleteStation(UUID locationId, UUID stationId);
    StationEntity getStationById(UUID id);
    StationEntity getStationByName(String stationName);
    List<StationEntity> getAllStations();
    StationEntity toggleActive(UUID locationId, UUID stationId, boolean active);
    //StationEntity updateStation(UUID id, StationEntity station);
    List<StationEntity> getStationsByLocation(UUID locationId);
    StationEntity updateStation(UUID locationId, UUID stationId, Map<String, Object> updates);
}

