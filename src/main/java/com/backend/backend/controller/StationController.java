package com.backend.backend.controller;

import com.backend.backend.config.UserContext;
import com.backend.backend.dto.StationDto;
import com.backend.backend.entity.StationEntity;
import com.backend.backend.entity.StationHistoryEntity;
import com.backend.backend.repositories.StationHistoryRepository;
import com.backend.backend.service.StationService;
import com.backend.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/stations")
@RequiredArgsConstructor
public class StationController {

    private final StationService stationService;
    private final UserService userService;
    private final StationHistoryRepository stationHistoryRepository;



    // ---------------- READ ----------------

//    @GetMapping
//    public ResponseEntity<List<StationEntity>> getAllStations() {
//        return ResponseEntity.ok(stationService.getAllStations());
//    }

    @GetMapping("/by-location/{locationId}")
    public ResponseEntity<List<StationDto>> getStationsByLocation(
            @PathVariable UUID locationId
    ) {
        return ResponseEntity.ok(stationService.getStationsByLocation(locationId));
    }

    @GetMapping("/by-name/{stationName}")
    public ResponseEntity<StationEntity> getStationByName(
            @PathVariable String stationName
    ) {
        return ResponseEntity.ok(stationService.getStationByName(stationName));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StationEntity> getStationById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(stationService.getStationById(id));
    }

    // ---------------- CREATE ----------------

    @PostMapping("/location/{locationId}")
    public ResponseEntity<StationEntity> createStation(
            @PathVariable UUID locationId,
            @RequestBody @Valid StationEntity station,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(
                stationService.createStation(locationId, station, userId)
        );
    }


    // ----------------  UPDATE ----------------

    @PutMapping("/{stationId}")
    public ResponseEntity<StationEntity> updateStation(
            @PathVariable UUID stationId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody Map<String, Object> updates
    ) {
        return ResponseEntity.ok(stationService.updateStation(stationId, updates, userId));
    }

    // ---------------- TOGGLE ACTIVE ----------------

    @PatchMapping("/{stationId}/active")
    public ResponseEntity<StationEntity> toggleActive(
            @PathVariable UUID stationId,
            @RequestParam boolean active,
            @RequestHeader("X-User-Id") UUID userId
    ) {
       return ResponseEntity.ok(stationService.toggleActive(stationId, active, userId));
    }

    // ---------------- DELETE (SOFT DELETE) ----------------

    @DeleteMapping("/{stationId}")
    public ResponseEntity<Void> deleteStation(

            @PathVariable UUID stationId,
            @RequestHeader("X-User-Id") UUID userId
    ) {

        UserContext.setCurrentUser(userId);

        stationService.deleteStation(stationId, userId);
        return ResponseEntity.noContent().build();
    }


    ///    reorder
    @PutMapping("/{locationId}/stations/reorder")
    public ResponseEntity<Void> reorderStations(
            @PathVariable UUID locationId,
            @RequestBody List<UUID> stationIdsInOrder,
            @RequestHeader("X-User-Id") UUID userId
    ) {

        stationService.reorderStations(locationId, stationIdsInOrder, userId);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/history")
    public List<StationHistoryEntity> getStationHistory(@RequestParam UUID locationId) {
        return stationHistoryRepository.findAllByLocationId(locationId);
    }



}

