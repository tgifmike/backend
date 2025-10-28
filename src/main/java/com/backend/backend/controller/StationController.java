package com.backend.backend.controller;

import com.backend.backend.entity.StationEntity;
import com.backend.backend.service.StationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/stations")
public class StationController {
    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<StationEntity>> getAllStations(){
        return ResponseEntity.ok(stationService.getAllStations());
    }

    @GetMapping("/{locationId}/getStationByLocation")
    public ResponseEntity<List<StationEntity>> getStationsByLocation(@PathVariable UUID locationId) {
        return ResponseEntity.ok(stationService.getStationsByLocation(locationId));
    }


    @GetMapping("/{stationName}")
        public ResponseEntity<StationEntity> getStationByName(@PathVariable String stationName){
        return ResponseEntity.ok(stationService.getStationByName(stationName));
        }

    @PostMapping("/{locationId}/createStation")
    public ResponseEntity<StationEntity> createStation(
            @PathVariable UUID locationId,
            @RequestBody StationEntity station
    ) {
        StationEntity created = stationService.createStation(locationId, station);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }


    @PatchMapping("/{locationId}/updateStation/{stationId}")
    public ResponseEntity<StationEntity> updateStation(
            @PathVariable UUID locationId,
            @PathVariable UUID stationId,
            @RequestBody Map<String, Object> updates
    ) {
        StationEntity updated = stationService.updateStation(locationId, stationId, updates);
        return ResponseEntity.ok(updated);
    }


    @DeleteMapping("/{locationId}/deleteStation/{stationId}")
    public ResponseEntity<Void> deleteStation(
            @PathVariable UUID locationId,
            @PathVariable UUID stationId
    ) {
        stationService.deleteStation(locationId, stationId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{locationId}/stations/{stationId}/active")
    public ResponseEntity<StationEntity> toggleStationActive(
            @PathVariable UUID locationId,
            @PathVariable UUID stationId,
            @RequestParam boolean active
    ) {
        StationEntity updated = stationService.toggleActive(locationId, stationId, active);
        return ResponseEntity.ok(updated);
    }


}
