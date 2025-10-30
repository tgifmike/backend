package com.backend.backend.serviceImplementation;

import com.backend.backend.dto.LocationDto;
import com.backend.backend.entity.AccountEntity;
import com.backend.backend.entity.LocationEntity;
import com.backend.backend.repositories.AccountRepository;
import com.backend.backend.repositories.LocationRepository;
import com.backend.backend.service.LocationService;
import jakarta.transaction.Transactional;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableInsertStrategy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final AccountRepository accountRepository;


    public LocationServiceImpl(LocationRepository locationRepository, AccountRepository accountRepository){
        this.locationRepository = locationRepository;
        this.accountRepository = accountRepository;

    }

    @Override
    public LocationEntity createLocation(UUID accountId, LocationDto locationDto) {

        if (locationRepository.existsByLocationNameAndAccountId(locationDto.getLocationName(), accountId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Location name already exists in this account");
        }

        // fetch account
        AccountEntity account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        // attach fields from DTO
        LocationEntity location = new LocationEntity();
        location.setAccount(account);
        location.setLocationName(locationDto.getLocationName());
        location.setLocationStreet(locationDto.getLocationStreet());
        location.setLocationTown(locationDto.getLocationTown());
        location.setLocationState(locationDto.getLocationState());
        location.setLocationZipCode(locationDto.getLocationZipCode());
        location.setLocationTimeZone(locationDto.getLocationTimeZone());


        return locationRepository.save(location);
    }


//    @Override
//    public LocationEntity updateLocation(UUID id, LocationDto updated) {
//        LocationEntity existing = locationRepository.findById(id)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
//
//        if (!existing.getLocationName().equals(updated.getLocationName())
//                && locationRepository.existsByLocationName(updated.getLocationName())){
//            throw new ResponseStatusException(HttpStatus.CONFLICT, "Location name already exists.");
//        }
//
//        existing.setLocationName(updated.getLocationName());
//        existing.setLocationStreet(updated.getLocationStreet());
//        existing.setLocationTown(updated.getLocationTown());
//        existing.setLocationState(updated.getLocationState());
//        existing.setLocationZipCode(updated.getLocationZipCode());
//        existing.setLocationTimeZone(updated.getLocationTimeZone());
//
//        // optionally handle latitude/longitude/geocode updates if needed
//        existing.setUpdatedAt(LocalDateTime.now());
//
//        return locationRepository.save(existing);
//    }

    @Override
    public LocationEntity updateLocation(UUID id, LocationEntity location){
        LocationEntity existing = locationRepository.findById(id)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));

        if (!existing.getLocationName().equals(location.getLocationName())
                && locationRepository.existsByLocationNameAndAccountId(location.getLocationName(), existing.getAccount().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Location name already exists in this account");
        }

        existing.setLocationName(location.getLocationName());
        existing.setLocationStreet(location.getLocationStreet());
        existing.setLocationTown(location.getLocationTown());
        existing.setLocationState(location.getLocationState());
        existing.setLocationZipCode(location.getLocationZipCode());
        existing.setLocationTimeZone(location.getLocationTimeZone());

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
    public LocationDto toggleActive(UUID id, boolean active) {
        LocationEntity location = locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found: " + id));

        location.setLocationActive(active);
        location.setUpdatedAt(LocalDateTime.now());

        LocationEntity saved = locationRepository.save(location);

        // Use the standardized converter
        return LocationDto.fromEntity(saved);
    }

    @Override
    public LocationEntity partialUpdate(UUID id, Map<String, Object> updates) {
        LocationEntity existing = locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found"));

        if (updates.containsKey("locationName") && updates.get("locationName") != null) existing.setLocationName((String) updates.get("locationName"));
        if (updates.containsKey("locationStreet") && updates.get("locationStreet") != null) existing.setLocationStreet((String) updates.get("locationStreet"));
        if (updates.containsKey("locationTown") && updates.get("locationTown") != null) existing.setLocationTown((String) updates.get("locationTown"));
        if (updates.containsKey("locationState") && updates.get("locationState") != null) existing.setLocationState((String) updates.get("locationState"));
        if (updates.containsKey("locationZipCode") && updates.get("locationZipCode") != null) existing.setLocationZipCode((String) updates.get("locationZipCode"));
        if (updates.containsKey("locationTimeZone") && updates.get("locationTimeZone") != null) existing.setLocationTimeZone((String) updates.get("locationTimeZone"));

        return locationRepository.save(existing);
    }

}
