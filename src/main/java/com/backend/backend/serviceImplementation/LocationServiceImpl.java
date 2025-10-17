package com.backend.backend.serviceImplementation;

import com.backend.backend.dto.LocationDto;
import com.backend.backend.entity.LocationEntity;
import com.backend.backend.repositories.LocationRepository;
import com.backend.backend.service.LocationService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;

    public LocationServiceImpl(LocationRepository locationRepository){
        this.locationRepository = locationRepository;
    }

    @Override
    public LocationEntity createLocation(LocationEntity location){
        if(locationRepository.existsByLocationName(location.getLocationName())){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Location name already exits");
        }
        return locationRepository.save(location);
    }

    @Override
    public LocationEntity updateLocation(UUID id, LocationEntity location){
        LocationEntity existing = locationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location no founds"));

        if(!existing.getLocationName().equals(location.getLocationName())
            && locationRepository.existsByLocationName(location.getLocationName())){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Location name already exists");
        }

        existing.setLocationName(location.getLocationName());
        existing.setLocationActive(location.isLocationActive());
        existing.setLocationStreet(location.getLocationStreet());
        existing.setLocationState(location.getLocationState());
        existing.setLocationTown(location.getLocationTown());
        existing.setLocationZipCode(location.getLocationZipCode());
        existing.setLocationTimezone(location.getLocationTimezone());
        existing.setLocationLatitude(location.getLocationLatitude());
        existing.setLocationLongitude(location.getLocationLongitude());

        return locationRepository.save(existing);
    }

    @Override
    public void deleteLocation(UUID id){
        if(!locationRepository.existsById(id)){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found");
        }
        locationRepository.deleteById(id);
    }

    @Override
    public LocationEntity getLocationById(UUID id){
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));
    }

    @Override
    public LocationEntity getLocationByName(String locationName){
        return locationRepository.findByLocationName(locationName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Locaiton not found"));
    }

    @Override
    public List<LocationEntity> getLocationByAccount(UUID accountId) {
        List<LocationEntity> locations = locationRepository.findByAccountId(accountId);
        if (locations.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No locations found for this account");
        }
        return locations;
    }

    @Override
    public List<LocationEntity> getAllLocations() {
        return  locationRepository.findAll();
    }

    @Override
    @Transactional
    public LocationDto toggleActive(UUID id, boolean active){
        LocationEntity location = locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found: " + id));

        location.setLocationActive(active);
        location.setUpdatedAt(LocalDateTime.now());

        LocationEntity saved = locationRepository.save(location);

        return new LocationDto(
                saved.getId(),
                saved.getLocationName(),
                saved.getLocationStreet(),
                saved.getLocationTown(),
                saved.getLocationState(),
                saved.getLocationZipCode(),
                saved.getLocationTimezone(),
                saved.isLocationActive()

        );
    }
}
