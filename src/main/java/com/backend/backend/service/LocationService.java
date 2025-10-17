package com.backend.backend.service;

import com.backend.backend.dto.LocationDto;
import com.backend.backend.entity.LocationEntity;

import java.util.List;
import java.util.UUID;

public interface LocationService {
    LocationEntity createLocation(LocationEntity location);
    LocationEntity updateLocation(UUID id, LocationEntity location);
    void deleteLocation(UUID id);
    LocationEntity getLocationById(UUID id);
    LocationEntity getLocationByName(String locationName);
    List<LocationEntity> getLocationByAccount(UUID accountId);
    List<LocationEntity> getAllLocations();
    LocationDto toggleActive(UUID id, boolean active);
}
