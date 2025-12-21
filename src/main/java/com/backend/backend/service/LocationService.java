package com.backend.backend.service;

import com.backend.backend.dto.LineCheckSettingsDto;
import com.backend.backend.dto.LocationDto;
import com.backend.backend.entity.LocationEntity;
import com.backend.backend.entity.UserEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface LocationService {

    LocationEntity createLocation(UUID accountId, LocationDto locationDto, UserEntity user);

    LocationEntity updateLocation(UUID id, LocationEntity location, UserEntity user);

    void deleteLocation(UUID id, UserEntity user);

    LocationDto toggleActive(UUID id, boolean active, UserEntity user);

    LocationEntity partialUpdate(UUID id, Map<String, Object> updates, UserEntity user);

    LocationEntity getLocationById(UUID id);

    LocationEntity getLocationByName(String locationName);

    List<LocationEntity> getLocationByAccount(UUID accountId);

    List<LocationEntity> getAllLocations();

    void updateGeocodeForLocation(UUID accountId, UUID locationId);

    void backfillLatLonForAllLocations();

    LineCheckSettingsDto getLineCheckSettings(UUID locationId);

    LineCheckSettingsDto updateLineCheckSettings(UUID locationId, LineCheckSettingsDto dto, UserEntity user);
}
