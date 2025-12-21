package com.backend.backend.controller;

import com.backend.backend.dto.LineCheckSettingsDto;
import com.backend.backend.dto.LocationDto;
import com.backend.backend.entity.LocationEntity;
import com.backend.backend.entity.LocationHistoryEntity;
import com.backend.backend.entity.UserEntity;
import com.backend.backend.repositories.LocationHistoryRepository;
import com.backend.backend.service.LocationService;
import com.backend.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/locations")
public class LocationController {

    private final LocationService locationService;
    private final UserService userService;
    private final LocationHistoryRepository locationHistoryRepository;

    public LocationController(LocationService locationService,
                              UserService userService,
                              LocationHistoryRepository locationHistoryRepository) {
        this.locationService = locationService;
        this.userService = userService;
        this.locationHistoryRepository = locationHistoryRepository;
    }

    @GetMapping("/getAllLocations")
    public ResponseEntity<List<LocationEntity>> getAllLocations() {
        return ResponseEntity.ok(locationService.getAllLocations());
    }

    @GetMapping("/accounts/{accountId}/locations")
    public ResponseEntity<List<LocationEntity>> getLocationsForAccount(@PathVariable UUID accountId) {
        return ResponseEntity.ok(locationService.getLocationByAccount(accountId));
    }

    @GetMapping("/{locationName}")
    public ResponseEntity<LocationEntity> getLocationByName(@PathVariable String locationName) {
        return ResponseEntity.ok(locationService.getLocationByName(locationName));
    }

    @PostMapping("/{accountId}/createLocation")
    public ResponseEntity<LocationEntity> createLocation(
            @PathVariable UUID accountId,
            @RequestParam UUID userId,
            @RequestBody LocationDto locationDto) {

        UserEntity user = userService.getUserById(userId);
        LocationEntity created = locationService.createLocation(accountId, locationDto, user);

        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/updateLocation")
    public ResponseEntity<LocationEntity> updateLocation(
            @PathVariable UUID id,
            @RequestParam UUID userId,
            @RequestBody Map<String, Object> updates) {

        UserEntity user = userService.getUserById(userId);
        LocationEntity updated = locationService.partialUpdate(id, updates, user);

        locationService.updateGeocodeForLocation(updated.getAccount().getId(), updated.getId());

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocation(
            @PathVariable UUID id,
            @RequestParam UUID userId
    ) {
        UserEntity user = userService.getUserById(userId);
        locationService.deleteLocation(id, user);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<LocationDto> toggleActive(
            @PathVariable UUID id,
            @RequestParam boolean active,
            @RequestParam UUID userId
    ) {
        UserEntity user = userService.getUserById(userId);
        LocationDto updated = locationService.toggleActive(id, active, user);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/accounts/{accountId}/locations/{locationId}/update-geocode")
    public ResponseEntity<?> updateLocationGeocode(
            @PathVariable UUID accountId,
            @PathVariable UUID locationId) {

        locationService.updateGeocodeForLocation(accountId, locationId);

        return ResponseEntity.ok(Map.of("message", "Geocode updated"));
    }

    @PostMapping("/locations/backfill-geocodes")
    public ResponseEntity<String> backfillGeocodes() {
        locationService.backfillLatLonForAllLocations();
        return ResponseEntity.ok("Geocoding backfill completed.");
    }

    @GetMapping("/{locationId}/line-check-settings")
    public ResponseEntity<LineCheckSettingsDto> getLineCheckSettings(@PathVariable UUID locationId) {
        LineCheckSettingsDto settings = locationService.getLineCheckSettings(locationId);
        return ResponseEntity.ok(settings);
    }

    @PutMapping("/{locationId}/line-check-settings")
    public ResponseEntity<LineCheckSettingsDto> updateLineCheckSettings(
            @PathVariable UUID locationId,
            @RequestParam UUID userId,
            @RequestBody LineCheckSettingsDto dto
    ) {
        UserEntity user = userService.getUserById(userId);
        LineCheckSettingsDto updated = locationService.updateLineCheckSettings(locationId, dto, user);
        return ResponseEntity.ok(updated);
    }

    /**
     * Get location history, optionally filtered by locationId
     */
    @GetMapping("/history")
    public ResponseEntity<List<LocationHistoryEntity>> getLocationHistory(
            @RequestParam(required = false) UUID accountId) {

        List<LocationHistoryEntity> history;

        if (accountId != null) {
            // history for all locations under the account
            history = locationHistoryRepository.findByLocation_Account_IdOrderByChangeAtDesc(accountId);
        } else {
            // all history globally
            history = locationHistoryRepository.findAllByOrderByChangeAtDesc();
        }

        return ResponseEntity.ok(history);
    }


}