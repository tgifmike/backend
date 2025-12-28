package com.backend.backend.serviceImplementation;

import com.backend.backend.dto.StationDto;
import com.backend.backend.entity.LocationEntity;
import com.backend.backend.entity.StationEntity;
import com.backend.backend.entity.StationHistoryEntity;
import com.backend.backend.entity.UserEntity;
import com.backend.backend.enums.HistoryType;
import com.backend.backend.repositories.LocationRepository;
import com.backend.backend.repositories.StationHistoryRepository;
import com.backend.backend.repositories.StationRepository;
import com.backend.backend.repositories.UserRepository;
import com.backend.backend.service.StationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

//import static jdk.internal.classfile.impl.DirectCodeBuilder.build;

@Slf4j
@Service
@RequiredArgsConstructor
public class StationServiceImpl implements StationService {

    private final StationRepository stationRepository;
    private final UserRepository userRepository;
    private final StationHistoryRepository stationHistoryRepository;
    private final LocationRepository locationRepository;

    private void recordHistory(
            StationEntity station,
            UUID changedBy,
            HistoryType changeType,
            Map<String, Object> oldValues
    ) {
        String changedByName = getUserNameById(changedBy);

        StationHistoryEntity history = StationHistoryEntity.builder()
                .station(station)
                .stationName(station.getStationName())
                .stationActive(station.isStationActive())
                .stationSortOrder(station.getSortOrder())
                .changeType(changeType)
                .changedBy(changedBy)
                .changedByName(changedByName)
                .changeAt(Instant.now())
                .locationId(station.getLocation() != null ? station.getLocation().getId() : null)
                .oldValues(oldValues != null
                        ? oldValues.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> Objects.toString(e.getValue(), "")
                        ))
                        : new HashMap<>())
                .build();

        stationHistoryRepository.save(history);
    }


    private String getUserNameById(UUID userId) {
        if (userId == null) return "System";
        return userRepository.findById(userId)
                .map(u -> u.getUserName() != null ? u.getUserName() : u.getId().toString())
                .orElse("Unknown User");
    }

    // ---------------- GETTERS ----------------
    private StationEntity getStationByIdAndLocation(UUID stationId, UUID locationId) {
        return stationRepository.findByIdAndLocation_Id(stationId, locationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Station not found in this location"));
    }

    @Override
    public StationEntity getStationById(UUID id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Station not found"));
    }

    @Override
    public StationEntity getStationByName(String name) {
        return stationRepository.findByStationNameIgnoreCase(name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Station not found"));
    }

    @Override
    public List<StationEntity> getAllStations() {
        return stationRepository.findAll();
    }

    @Override
    public List<StationDto> getStationsByLocation(UUID locationId) {
        List<StationEntity> stations = stationRepository.findByLocation_IdOrderBySortOrderAsc(locationId);
        return stations.stream()
                .map(station -> new StationDto(
                        station.getId(),
                        station.getStationName(),
                        station.isStationActive(),
                        station.getSortOrder(),
                        List.of()
                ))
                .collect(Collectors.toList());
    }

    // ---------------- CREATE ----------------
    @Override
    @Transactional
    public StationEntity createStation(UUID locationId, StationEntity station, UUID userId) {
        if (locationId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location ID is required");
        }

        // Fetch the location
        LocationEntity location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location not found"));

        // Assign it to the station
        station.setLocation(location);

        station.setCreatedBy(userId);
        station.setCreatedAt(Instant.now());

        StationEntity saved = stationRepository.save(station);

        recordHistory(saved, userId, HistoryType.CREATED, null);

        return saved;
    }



    // ---------------- FULL UPDATE ----------------
    @Override
    @Transactional
    public StationEntity updateStation(UUID stationId, Map<String, Object> updates, UUID userId) {
        StationEntity existing = stationRepository.findById(stationId)
                .orElseThrow(() -> new NoSuchElementException("Station not found"));

        Map<String, Object> oldValues = new HashMap<>();

        if (updates.containsKey("stationName")) {
            oldValues.put("stationName", existing.getStationName());
            existing.setStationName((String) updates.get("stationName"));
        }

        // add other partial updates if needed

        existing.setUpdatedBy(userId);
        existing.setUpdatedAt(Instant.now());

        StationEntity saved = stationRepository.save(existing);

        if (!oldValues.isEmpty()) {
            recordHistory(saved, userId, HistoryType.UPDATED, oldValues);
        }

        return saved;
    }

    // ---------------- DELETE ----------------
    @Override
    @Transactional
    public void deleteStation(UUID stationId, UUID userId) {

        StationEntity station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NoSuchElementException("Station not found"));

        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("stationName", station.getStationName());
        oldValues.put("sortOrder", station.getSortOrder());
        oldValues.put("stationActive", station.isStationActive());

        log.info("Deleting station: id={} oldName={} setting deletedAt={} deletedBy={}",
                station.getId(), station.getStationName(), Instant.now(), userId);

        station.setDeletedAt(Instant.now());
        station.setDeletedBy(userId);
        station.setUpdatedAt(Instant.now());

        stationRepository.saveAndFlush(station);

        log.info("After save: deletedAt={} deletedBy={}", station.getDeletedAt(), station.getDeletedBy());


        recordHistory(station, userId, HistoryType.DELETED, oldValues);
    }

    @Override
    @Transactional
    public void reorderStations(
            UUID locationId,
            List<UUID> orderedIds,
            UUID userId
    ) {

        List<StationEntity> stations =
                stationRepository.findByLocation_IdOrderBySortOrderAsc(locationId);

        Map<UUID, StationEntity> map = stations.stream()
                .collect(Collectors.toMap(StationEntity::getId, s -> s));

        for (int i = 0; i < orderedIds.size(); i++) {
            StationEntity sta = map.get(orderedIds.get(i));

            if (sta != null && !Objects.equals(sta.getSortOrder(), i)) {
                Map<String, Object> oldValues = new HashMap<>();
                oldValues.put("sortOrder", sta.getSortOrder());

                sta.setSortOrder(i);
                sta.setUpdatedAt(Instant.now());
                sta.setUpdatedBy(userId);

                recordHistory(sta, userId, HistoryType.UPDATED, oldValues);
            }
        }

        stationRepository.saveAll(stations);
    }


    // ---------------- TOGGLE ACTIVE ----------------
    @Override
    @Transactional
    public StationEntity toggleActive(UUID stationId, boolean active, UUID userId) {

        StationEntity station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NoSuchElementException("Station not found"));

        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("stationActive", station.isStationActive());

        station.setStationActive(active);
        station.setUpdatedBy(userId);
        station.setUpdatedAt(Instant.now());

        StationEntity saved = stationRepository.save(station);

        recordHistory(saved, userId, HistoryType.UPDATED, oldValues);

        return saved;

    }

}

