package com.backend.backend.controller;

import com.backend.backend.dto.StationDto;
import com.backend.backend.entity.StationEntity;
import com.backend.backend.repositories.StationRepository;
import com.backend.backend.service.StationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/stations")
public class StationController {
    private final StationService stationService;
    private final StationRepository stationRepository;

    public StationController(StationService stationService, StationRepository stationRepository) {
        this.stationService = stationService;
        this.stationRepository = stationRepository;
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<StationEntity>> getAllStations(){
        return ResponseEntity.ok(stationService.getAllStations());
    }

    @GetMapping("/{locationId}/getStationByLocation")
    public ResponseEntity<List<StationDto>> getStationsByLocation(@PathVariable UUID locationId) {
        List<StationDto> stations = stationService.getStationsByLocation(locationId);
        return ResponseEntity.ok(stations);
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

    //reodering
    @PutMapping("/{locationId}/stations/reorder")
    public ResponseEntity<Void> reorderItems(
            @PathVariable UUID locationId,
            @RequestBody List<UUID> stationIdsInOrder
    ) {
        List<StationEntity> stations = stationRepository.findAllById(stationIdsInOrder);

        // Create a lookup map for items by ID
        Map<UUID, StationEntity> stationMap = stations.stream()
                .collect(Collectors.toMap(StationEntity::getId, Function.identity()));

        // Assign new order directly
        for (int i = 0; i < stationIdsInOrder.size(); i++) {
            UUID stationId = stationIdsInOrder.get(i);
            StationEntity station = stationMap.get(stationId);
            if (station != null) {
                station.setSortOrder(i + 1);
            }
        }

        stationRepository.saveAll(stations);
        return ResponseEntity.ok().build();
    }

}
