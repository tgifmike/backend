package com.backend.backend.controller;

import com.backend.backend.dto.LocationDto;
import com.backend.backend.entity.*;
import com.backend.backend.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/user-access-locations")
public class UserLocationAccessController {

    private final UserLocationAccessService userLocationAccessService;
    private final UserService userService;
    private final LocationService locationService;

    public UserLocationAccessController(UserLocationAccessService userLocationAccessService, UserService userService, LocationService locationService) {
        this.userLocationAccessService = userLocationAccessService;
        this.userService = userService;
        this.locationService = locationService;
    }

//    @GetMapping("/{userId}/locations")
//    public ResponseEntity<List<LocationDto>> getLocationsForUser(@PathVariable UUID userId) {
//        UserEntity user = userService.getUserById(userId);
//        List<LocationDto> locations = userLocationAccessService.getLocationsForUser(user)
//                .stream()
//                .map(access -> new LocationDto(
//                        access.getLocation().getId(),
//                        access.getLocation().getLocationName(),
//                        access.getLocation().isLocationActive()
//                ))
//                .toList();
//        return ResponseEntity.ok(locations);
//    }

    @GetMapping("/{userId}/locations")
    public ResponseEntity<List<LocationDto>> getLocationsForUser(@PathVariable UUID userId) {
        List<LocationEntity> locations = userLocationAccessService.getLocationsForUser(userId);
        List<LocationDto> dtos = locations.stream()
                .map(LocationDto::fromEntity)
                .toList();
        return ResponseEntity.ok(dtos);
    }



    @PostMapping("/{userId}/locations/{locationId}")
    public UserLocationAccessEntity grantAccess(@PathVariable UUID userId,
                                                @PathVariable UUID locationId) {
        UserEntity user = userService.getUserById(userId);
        LocationEntity location = locationService.getLocationById(locationId);
        return userLocationAccessService.grantAccess(user, location);
    }

    @DeleteMapping("/{userId}/locations/{locationId}")
    public void revokeAccess(@PathVariable UUID userId,
                             @PathVariable UUID locationId) {
        UserEntity user = userService.getUserById(userId);
        LocationEntity location = locationService.getLocationById(locationId);
        userLocationAccessService.revokeAccess(user, location);
    }



}