package com.backend.backend.serviceImplementation;

import com.backend.backend.entity.LocationEntity;
import com.backend.backend.entity.StationEntity;
import com.backend.backend.repositories.LocationRepository;
import com.backend.backend.repositories.StationRepository;
import com.backend.backend.service.StationService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class StationServiceImpl implements StationService {

    private final StationRepository stationRepository;
    private final LocationRepository locationRepository;

    public StationServiceImpl(StationRepository stationRepository, LocationRepository locationRepository) {
        this.stationRepository = stationRepository;
        this.locationRepository = locationRepository;
    }

    @Override
    public StationEntity createStation(UUID locationId, StationEntity station) {
        if (stationRepository.existsByStationNameAndLocation_Id(station.getStationName(), locationId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Station name already exists for this location");
        }

        LocationEntity location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));

        station.setLocation(location);
        return stationRepository.save(station);
    }

    @Override
    @Transactional
    public StationEntity updateStation(UUID locationId, UUID stationId, Map<String, Object> updates) {
        StationEntity existing = stationRepository.findByIdAndLocation_Id(stationId, locationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Station not found in this location"));

        if (updates.containsKey("stationName") && updates.get("stationName") != null) {
            String newName = (String) updates.get("stationName");
            if (!existing.getStationName().equals(newName) &&
                    stationRepository.existsByStationNameAndLocation_Id(newName, locationId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Station name already exists in this location");
            }
            existing.setStationName(newName);
        }

        existing.setUpdatedAt(LocalDateTime.now());
        return stationRepository.save(existing);
    }

    @Override
    @Transactional
    public StationEntity toggleActive(UUID locationId, UUID stationId, boolean active) {
        StationEntity station = stationRepository.findByIdAndLocation_Id(stationId, locationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Station not found in this location"));

        station.setStationActive(active);
        station.setUpdatedAt(LocalDateTime.now());
        return stationRepository.save(station);
    }

    @Override
    public void deleteStation(UUID locationId, UUID stationId) {
        StationEntity station = stationRepository.findByIdAndLocation_Id(stationId, locationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Station not found in this location"));

        stationRepository.delete(station);
    }



    @Override
    public StationEntity getStationById(UUID id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Station not found"));
    }

    @Override
    public StationEntity getStationByName(String stationName) {
        return stationRepository.findByStationName(stationName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Station not found"));
    }

    @Override
    public List<StationEntity> getAllStations() {
        return stationRepository.findAll();
    }

    @Override
    public List<StationEntity> getStationsByLocation(UUID locationId) {
        List<StationEntity> stations = stationRepository.findByLocation_Id(locationId);
        if (stations.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No stations found for this location");
        }
        return stations;
    }
}