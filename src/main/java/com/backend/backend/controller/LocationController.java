package com.backend.backend.controller;

import com.backend.backend.dto.LineCheckSettingsDto;
import com.backend.backend.dto.LocationDto;
import com.backend.backend.entity.LocationEntity;
import com.backend.backend.service.LocationService;
import com.backend.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/locations")
public class LocationController {

    private final LocationService locationService;
    private final UserService userService;

    public LocationController(LocationService locationService, UserService userService){
        this.locationService = locationService;
        this.userService = userService;
    }

    @GetMapping("/getAllLocations")
    public ResponseEntity<List<LocationEntity>> getAllLocations(){
        return ResponseEntity.ok(locationService.getAllLocations());
    }

    //get location for account
    @GetMapping("/accounts/{accountId}/locations")
    public ResponseEntity<List<LocationEntity>> getLocationsForAccount(@PathVariable UUID accountId) {
        return ResponseEntity.ok(locationService.getLocationByAccount(accountId));
    }

    @GetMapping("/{locationName}")
    public ResponseEntity<LocationEntity> getLocationByName(@PathVariable String locationName){
        return ResponseEntity.ok(locationService.getLocationByName(locationName));
    }

    @PostMapping("/{accountId}/createLocation")
    public ResponseEntity<LocationEntity> createLocation(
            @PathVariable UUID accountId,
            @RequestBody LocationDto locationDto) {
        LocationEntity created = locationService.createLocation(accountId, locationDto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/updateLocation")
    public ResponseEntity<LocationEntity> updateLocation(@PathVariable UUID id, @RequestBody Map<String, Object> updates) {
        LocationEntity updated = locationService.partialUpdate(id, updates);

        locationService.updateGeocodeForLocation(updated.getAccount().getId(), updated.getId());

        return ResponseEntity.ok(updated);
    }



    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocation(@PathVariable UUID id){
        locationService.deleteLocation(id);
        return ResponseEntity.noContent().build();
    }

    //update status
    @PatchMapping("/{id}/active")
    public ResponseEntity<LocationDto> toggleActive(
            @PathVariable UUID id,
            @RequestParam boolean active
    ){
        LocationDto updated = locationService.toggleActive(id, active);
        return ResponseEntity.ok(updated);
    }

    //update lat/long
    @PostMapping("/accounts/{accountId}/locations/{locationId}/update-geocode")
    public ResponseEntity<?> updateLocationGeocode(
            @PathVariable UUID accountId,
            @PathVariable UUID locationId) {

        locationService.updateGeocodeForLocation(accountId, locationId);

        return ResponseEntity.ok(Map.of("message", "Geocode updated"));
    }

    //backfill geocodes
    @PostMapping("/locations/backfill-geocodes")
    public ResponseEntity<String> backfillGeocodes() {
        locationService.backfillLatLonForAllLocations();
        return ResponseEntity.ok("Geocoding backfill completed.");
    }

    // Get line check settings for a location
    @GetMapping("/{locationId}/line-check-settings")
    public ResponseEntity<LineCheckSettingsDto> getLineCheckSettings(@PathVariable UUID locationId) {
        LineCheckSettingsDto settings = locationService.getLineCheckSettings(locationId);
        return ResponseEntity.ok(settings);
    }

    // Update line check settings for a location
    @PutMapping("/{locationId}/line-check-settings")
    public ResponseEntity<LineCheckSettingsDto> updateLineCheckSettings(
            @PathVariable UUID locationId,
            @RequestBody LineCheckSettingsDto dto
    ) {
        LineCheckSettingsDto updated = locationService.updateLineCheckSettings(locationId, dto);
        return ResponseEntity.ok(updated);
    }

}
