package com.backend.backend.serviceImplementation;

import com.backend.backend.dto.LocationDto;
import com.backend.backend.entity.AccountEntity;
import com.backend.backend.entity.LocationEntity;
import com.backend.backend.repositories.AccountRepository;
import com.backend.backend.repositories.LocationRepository;
import com.backend.backend.service.GeocodingService;
import com.backend.backend.service.LocationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final AccountRepository accountRepository;
    private final GeocodingService geocodingService;


    public LocationServiceImpl(LocationRepository locationRepository, AccountRepository accountRepository, GeocodingService geocodingService){
        this.locationRepository = locationRepository;
        this.accountRepository = accountRepository;
        this.geocodingService = geocodingService;

    }

    @Override
    @Transactional
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


        // Save first (so we have an ID for geocoding updates)
        LocationEntity saved = locationRepository.saveAndFlush(location);

        // Run geocoding
        updateGeocodeForLocation(accountId, saved.getId());

        return locationRepository.findById(saved.getId()).orElse(saved);
    }


    @Transactional
    @Override
    public LocationEntity updateLocation(UUID id, LocationEntity location) {
        LocationEntity existing = locationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));

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
        existing.setLocationLongitude(location.getLocationLongitude());
        existing.setLocationLatitude(location.getLocationLatitude());
        existing.setGeocodedFromZipFallback(location.getGeocodedFromZipFallback());

        // Save first
        locationRepository.saveAndFlush(existing);

        // ✅ Trigger geocoding after save
        updateGeocodeForLocation(existing.getAccount().getId(), existing.getId());

        return existing;
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
        if (updates.containsKey("locationLatitude") && updates.get("locationLatitude") != null) existing.setLocationLatitude((double) updates.get("locationLatitude"));
        if (updates.containsKey("locationLongitude") && updates.get("locationLongitude") != null) existing.setLocationLongitude((double) updates.get("locationLongitude"));
        if (updates.containsKey("GeocodedFromZipFallback") && updates.get("GeocodedFromZipFallback") != null) existing.setGeocodedFromZipFallback((boolean) updates.get("GeocodedFromZipFallback"));


        return locationRepository.save(existing);
    }

    @Override
    public void updateGeocodeForLocation(UUID accountId, UUID locationId) {
        LocationEntity location = locationRepository.findById(locationId)
                .orElseThrow(() -> new EntityNotFoundException("Location not found with id: " + locationId));

        if (!location.getAccount().getId().equals(accountId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location does not belong to the given account");
        }

        String fullAddress = buildFullAddress(location);
        System.out.println("➡️ Full address: " + fullAddress);
        System.out.println("➡️ Geocoding query: " + fullAddress);

        Optional<GeocodingService.GeocodeResult> geoResult =
                geocodingService.getLatLongFromAddressWithFallback(fullAddress, location.getLocationZipCode());

        if (geoResult.isPresent()) {
            GeocodingService.GeocodeResult result = geoResult.get();
            location.setLocationLatitude(result.latitude());
            location.setLocationLongitude(result.longitude());
            location.setGeocodedFromZipFallback(result.fromZipFallback());
            locationRepository.save(location);
            System.out.println("✅ Geocode: " + result.latitude() + ", " + result.longitude() + " (fallback=" + result.fromZipFallback() + ")");
        } else {
            System.out.println("⚠️ No geocode result for: " + fullAddress);
        }
    }
    private String buildFullAddress(LocationEntity location) {
        String street = Optional.ofNullable(location.getLocationStreet()).orElse("").trim();
        String town = Optional.ofNullable(location.getLocationTown()).orElse("").trim();
        String state = Optional.ofNullable(location.getLocationState()).orElse("").trim();
        String zip = Optional.ofNullable(location.getLocationZipCode()).orElse("").trim();

        // Prefer full street address if available
        if (!street.isEmpty()) {
            return String.join(", ", street, town, state, zip, "USA").replaceAll(", ,", ",").replaceAll(", $", "");
        }

        // Otherwise try town+state
        if (!town.isEmpty() && !state.isEmpty()) {
            return String.join(", ", town, state, "USA");
        }

        // Fallback to ZIP
        if (!zip.isEmpty()) {
            return zip + ", USA";
        }

        // As last resort, country only
        return "USA";
    }


    @Override
    public void backfillLatLonForAllLocations() {
        List<LocationEntity> locations = locationRepository.findAll();

        for (LocationEntity location : locations) {
            if (location.getLocationLatitude() == null && location.getLocationZipCode() != null) {
                String fullAddress = buildFullAddress(location);
                Optional<GeocodingService.GeocodeResult> geoResult =
                        geocodingService.getLatLongFromAddressWithFallback(fullAddress, location.getLocationZipCode());

                geoResult.ifPresent(result -> {
                    location.setLocationLatitude(result.latitude());
                    location.setLocationLongitude(result.longitude());
                    location.setGeocodedFromZipFallback(result.fromZipFallback());
                    locationRepository.save(location);
                });
            }
        }
    }
}
