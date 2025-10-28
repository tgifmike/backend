package com.backend.backend.controller;

import com.backend.backend.entity.StationEntity;
import com.backend.backend.service.StationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;
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

    @GetMapping("/{stationName}")
        public ResponseEntity<StationEntity> getStationByName(@PathVariable String stationName){
        return ResponseEntity.ok(stationService.getStationByName(stationName));
        }

    @PostMapping("/createStation")
    public ResponseEntity<StationEntity> createStation(@RequestBody StationEntity station){
        return new ResponseEntity<>(stationService.createStation(station), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<StationEntity> updateStation(@PathVariable UUID id, @RequestBody StationEntity station){
        StationEntity updated = stationService.updateStation(id, station);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStation(@PathVariable UUID id){
        stationService.deleteStation(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<StationEntity> toggleActive(
            @PathVariable UUID id,
            @RequestParam boolean active
    ) {
        StationEntity updated = stationService.toggleActive(id, active);
        return ResponseEntity.ok(updated);
    }

}
