package com.backend.backend.service;

import com.backend.backend.dto.LineCheckSettingsDto;
import com.backend.backend.dto.LocationDto;
import com.backend.backend.entity.LocationEntity;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface LocationService {
    LocationEntity updateLocation(UUID id, LocationEntity location);
    LocationEntity createLocation(UUID accountId, LocationDto locationDto);
    void deleteLocation(UUID id);
    LocationEntity getLocationById(UUID id);
    LocationEntity getLocationByName(String locationName);
    List<LocationEntity> getLocationByAccount(UUID accountId);
    List<LocationEntity> getAllLocations();
    LocationDto toggleActive(UUID id, boolean active);
    LocationEntity partialUpdate(UUID id, Map<String, Object> updates);
    void updateGeocodeForLocation(UUID accountId, UUID locationId);
    void backfillLatLonForAllLocations();
    LineCheckSettingsDto getLineCheckSettings(UUID locationId);
    LineCheckSettingsDto updateLineCheckSettings(UUID locationId, LineCheckSettingsDto dto);
}
