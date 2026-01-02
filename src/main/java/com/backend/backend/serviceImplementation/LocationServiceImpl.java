package com.backend.backend.serviceImplementation;

import com.backend.backend.config.StartOfWeek;
import com.backend.backend.dto.LineCheckSettingsDto;
import com.backend.backend.dto.LocationDto;
import com.backend.backend.entity.AccountEntity;
import com.backend.backend.entity.LocationEntity;
import com.backend.backend.entity.LocationHistoryEntity;
import com.backend.backend.entity.UserEntity;
import com.backend.backend.repositories.AccountRepository;
import com.backend.backend.repositories.LocationHistoryRepository;
import com.backend.backend.repositories.LocationRepository;
import com.backend.backend.service.GeocodingService;
import com.backend.backend.service.LocationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final GeocodingService geocodingService;
    private final AccountRepository accountRepository;
    private final LocationHistoryRepository locationHistoryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LocationServiceImpl(LocationRepository locationRepository,
                               GeocodingService geocodingService,
                               AccountRepository accountRepository,
                               LocationHistoryRepository locationHistoryRepository) {
        this.locationRepository = locationRepository;
        this.geocodingService = geocodingService;
        this.accountRepository = accountRepository;
        this.locationHistoryRepository = locationHistoryRepository;
    }

    // ---------------- CREATE ----------------
    @Override
    @Transactional
    public LocationEntity createLocation(UUID accountId, LocationDto locationDto, UserEntity user) {
        String normalizedName = locationDto.getLocationName().trim();

        // Log input
        System.out.println("[CREATE LOCATION] accountId=" + accountId + ", name=" + normalizedName);

        // Check for duplicates
        boolean exists = locationRepository.existsActiveByLocationNameIgnoreCaseAndAccountId(normalizedName, accountId);
        System.out.println("[CREATE LOCATION] Duplicate exists? " + exists);

        if (exists) {
            System.out.println("[CREATE LOCATION] Conflict! Location already exists");
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Location name already exists in this account");
        }

        AccountEntity account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        LocationEntity location = new LocationEntity();
        location.setAccount(account);
        location.setLocationName(normalizedName);
        location.setLocationStreet(locationDto.getLocationStreet());
        location.setLocationTown(locationDto.getLocationTown());
        location.setLocationState(locationDto.getLocationState());
        location.setLocationZipCode(locationDto.getLocationZipCode());
        location.setLocationTimeZone(locationDto.getLocationTimeZone());
        location.setCreatedBy(user != null ? user.getId() : null);
        location.setUpdatedBy(user != null ? user.getId() : null);

        LocationEntity saved = locationRepository.saveAndFlush(location);

        System.out.println("[CREATE LOCATION] Saved locationId=" + saved.getId());

        saveLocationHistory(saved, null, extractLocationFields(saved), "CREATED", user);
        updateGeocodeForLocation(accountId, saved.getId());

        return saved;
    }




    // ---------------- UPDATE ----------------
    @Override
    @Transactional
    public LocationEntity updateLocation(UUID id, LocationEntity incoming, UserEntity user) {
        LocationEntity existing = getLocationById(id);

        String newName = incoming.getLocationName().trim();
        if (!existing.getLocationName().equalsIgnoreCase(newName)
                && locationRepository.existsByLocationNameIgnoreCaseAndAccount_Id(newName, existing.getAccount().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Location name already exists in this account");
        }

        Map<String, String> oldValues = extractLocationFields(existing);

        existing.setLocationName(newName);
        existing.setLocationStreet(incoming.getLocationStreet());
        existing.setLocationTown(incoming.getLocationTown());
        existing.setLocationState(incoming.getLocationState());
        existing.setLocationZipCode(incoming.getLocationZipCode());
        existing.setLocationTimeZone(incoming.getLocationTimeZone());
        existing.setLocationLatitude(incoming.getLocationLatitude());
        existing.setLocationLongitude(incoming.getLocationLongitude());
        existing.setGeocodedFromZipFallback(incoming.getGeocodedFromZipFallback());
        existing.setUpdatedBy(user != null ? user.getId() : null);
        existing.setUpdatedAt(Instant.now());

        LocationEntity saved = locationRepository.saveAndFlush(existing);

        saveLocationHistory(saved, oldValues, extractLocationFields(saved), "UPDATED", user);

        updateGeocodeForLocation(existing.getAccount().getId(), existing.getId());

        return saved;
    }

    // ---------------- PARTIAL UPDATE ----------------
    @Override
    @Transactional
    public LocationEntity partialUpdate(UUID id, Map<String, Object> updates, UserEntity user) {
        // Fetch existing location
        LocationEntity existing = getLocationById(id);

        // Track changes for history
        Map<String, Object> oldVals = new HashMap<>();
        Map<String, Object> newVals = new HashMap<>();

        // Iterate through updates and apply changes safely
        updates.forEach((key, value) -> {
            switch (key) {
                case "locationName" -> updateIfChanged(
                        existing.getLocationName(),
                        value != null ? value.toString().trim() : null,
                        existing::setLocationName,
                        key, oldVals, newVals
                );
                case "locationStreet" -> updateIfChanged(
                        existing.getLocationStreet(),
                        value != null ? value.toString() : null,
                        existing::setLocationStreet,
                        key, oldVals, newVals
                );
                case "locationTown" -> updateIfChanged(
                        existing.getLocationTown(),
                        value != null ? value.toString() : null,
                        existing::setLocationTown,
                        key, oldVals, newVals
                );
                case "locationState" -> updateIfChanged(
                        existing.getLocationState(),
                        value != null ? value.toString() : null,
                        existing::setLocationState,
                        key, oldVals, newVals
                );
                case "locationZipCode" -> updateIfChanged(
                        existing.getLocationZipCode(),
                        value != null ? value.toString() : null,
                        existing::setLocationZipCode,
                        key, oldVals, newVals
                );
                case "locationTimeZone" -> updateIfChanged(
                        existing.getLocationTimeZone(),
                        value != null ? value.toString() : null,
                        existing::setLocationTimeZone,
                        key, oldVals, newVals
                );
                case "locationActive" -> updateIfChanged(
                        existing.getLocationActive(),
                        value != null ? (Boolean) value : null,
                        existing::setLocationActive,
                        key, oldVals, newVals
                );
                case "lineCheckDailyGoal" -> updateIfChanged(
                        existing.getLineCheckDailyGoal(),
                        value != null ? ((Number) value).intValue() : null,
                        existing::setLineCheckDailyGoal,
                        key, oldVals, newVals
                );
                case "startOfWeek" -> updateIfChanged(
                        existing.getStartOfWeek(),
                        value != null ? StartOfWeek.valueOf(value.toString()) : null,
                        existing::setStartOfWeek,
                        key, oldVals, newVals
                );
                case "locationLatitude" -> updateIfChanged(
                        existing.getLocationLatitude(),
                        value != null ? ((Number) value).doubleValue() : null,
                        existing::setLocationLatitude,
                        key, oldVals, newVals
                );
                case "locationLongitude" -> updateIfChanged(
                        existing.getLocationLongitude(),
                        value != null ? ((Number) value).doubleValue() : null,
                        existing::setLocationLongitude,
                        key, oldVals, newVals
                );
                case "geocodedFromZipFallback" -> updateIfChanged(
                        existing.getGeocodedFromZipFallback(),
                        value != null ? (Boolean) value : null,
                        existing::setGeocodedFromZipFallback,
                        key, oldVals, newVals
                );
            }
        });

        // Update timestamps and user
        existing.setUpdatedBy(user != null ? user.getId() : null);
        existing.setUpdatedAt(Instant.now());

        // Save changes
        LocationEntity saved = locationRepository.save(existing);

        // Save history only if there were actual changes
        if (!oldVals.isEmpty()) {
            saveLocationHistory(
                    saved,
                    convertMapToString(oldVals),
                    convertMapToString(newVals),
                    "UPDATED",
                    user
            );
        }

        return saved;
    }

    // Generic helper method for updating if changed
    private <T> void updateIfChanged(T oldValue, T newValue, Consumer<T> setter,
                                     String key, Map<String, Object> oldVals, Map<String, Object> newVals) {
        if (!Objects.equals(oldValue, newValue)) {
            oldVals.put(key, oldValue);
            newVals.put(key, newValue);
            setter.accept(newValue);
        }
    }

    // Convert Map<String, Object> -> Map<String, String> safely
    private Map<String, String> convertMapToString(Map<String, Object> map) {
        if (map == null) return Map.of();
        return map.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue() != null ? e.getValue().toString() : null
                ));
    }


    // ---------------- DELETE ----------------
    @Override
    @Transactional
    public void deleteLocation(UUID id, UserEntity user) {
        LocationEntity location = getLocationById(id);
        Map<String, String> oldVals = extractLocationFields(location);

        // Soft delete
        location.setDeletedAt(Instant.now());
        location.setDeletedBy(user.getId());
        locationRepository.save(location);  // Persist changes

        // Save history after marking deleted
        saveLocationHistory(location, oldVals, null, "DELETED", user);
    }


    // ---------------- TOGGLE ACTIVE ----------------
    @Override
    @Transactional
    public LocationDto toggleActive(UUID id, boolean active, UserEntity user) {
        LocationEntity location = getLocationById(id);

        Map<String, String> oldVals = Map.of("locationActive", String.valueOf(location.getLocationActive()));
        location.setLocationActive(active);
        location.setUpdatedBy(user != null ? user.getId() : null);
        location.setUpdatedAt(Instant.now());

        LocationEntity saved = locationRepository.save(location);
        saveLocationHistory(saved, oldVals, Map.of("locationActive", String.valueOf(active)), "UPDATED", user);

        return LocationDto.fromEntity(saved);
    }

    // ---------------- GETTERS ----------------
    @Override
    public LocationEntity getLocationById(UUID id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));
    }

    @Override
    public LocationEntity getLocationByName(String locationName) {
        return locationRepository.findByLocationName(locationName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));
    }

    @Override
    public List<LocationEntity> getLocationByAccount(UUID accountId) {
        List<LocationEntity> locations = locationRepository.findByAccount_Id(accountId);
        if (locations.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No locations found");
        return locations;
    }

    @Override
    public List<LocationEntity> getAllLocations() {
        return locationRepository.findAll();
    }

    // ---------------- GEOCODING ----------------
    @Override
    public void updateGeocodeForLocation(UUID accountId, UUID locationId) {
        LocationEntity location = getLocationById(locationId);
        if (!location.getAccount().getId().equals(accountId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location does not belong to account");
        }

        String fullAddress = buildFullAddress(location);
        geocodingService.getLatLongFromAddressWithFallback(fullAddress, location.getLocationZipCode())
                .ifPresent(result -> {
                    location.setLocationLatitude(result.latitude());
                    location.setLocationLongitude(result.longitude());
                    location.setGeocodedFromZipFallback(result.fromZipFallback());
                    locationRepository.save(location);
                });
    }

    @Override
    public void backfillLatLonForAllLocations() {
        for (LocationEntity location : locationRepository.findAll()) {
            if (location.getLocationLatitude() == null && location.getLocationZipCode() != null) {
                String fullAddress = buildFullAddress(location);
                geocodingService.getLatLongFromAddressWithFallback(fullAddress, location.getLocationZipCode())
                        .ifPresent(result -> {
                            location.setLocationLatitude(result.latitude());
                            location.setLocationLongitude(result.longitude());
                            location.setGeocodedFromZipFallback(result.fromZipFallback());
                            locationRepository.save(location);
                        });
            }
        }
    }

    // ---------------- LINE CHECK SETTINGS ----------------
    @Override
    public LineCheckSettingsDto getLineCheckSettings(UUID locationId) {
        LocationEntity location = getLocationById(locationId);
        LineCheckSettingsDto dto = new LineCheckSettingsDto();
        dto.setDayOfWeek(location.getStartOfWeek() != null ? location.getStartOfWeek().name() : StartOfWeek.MONDAY.name());
        dto.setDailyGoal(location.getLineCheckDailyGoal() != null ? location.getLineCheckDailyGoal() : 1);
        return dto;
    }

    @Override
    @Transactional
    public LineCheckSettingsDto updateLineCheckSettings(
            UUID locationId,
            LineCheckSettingsDto dto,
            UserEntity user
    ) {
        LocationEntity location = getLocationById(locationId);

        Map<String, String> oldValues = new HashMap<>();
        Map<String, String> newValues = new HashMap<>();

        boolean changed = false;

        // ---- Start of Week ----
        if (dto.getDayOfWeek() != null && !dto.getDayOfWeek().isBlank()) {
            StartOfWeek newStart = StartOfWeek.valueOf(dto.getDayOfWeek().toUpperCase());

            if (!Objects.equals(location.getStartOfWeek(), newStart)) {
                oldValues.put(
                        "startOfWeek",
                        location.getStartOfWeek() != null
                                ? location.getStartOfWeek().name()
                                : null
                );
                newValues.put("startOfWeek", newStart.name());

                location.setStartOfWeek(newStart);
                changed = true;
            }
        }

        // ---- Daily Line Check Goal ----
        if (dto.getDailyGoal() != null
                && dto.getDailyGoal() > 0
                && !Objects.equals(location.getLineCheckDailyGoal(), dto.getDailyGoal())) {

            oldValues.put(
                    "lineCheckDailyGoal",
                    location.getLineCheckDailyGoal() != null
                            ? location.getLineCheckDailyGoal().toString()
                            : null
            );
            newValues.put("lineCheckDailyGoal", dto.getDailyGoal().toString());

            location.setLineCheckDailyGoal(dto.getDailyGoal());
            changed = true;
        }

        // ---- Persist + History ----
        if (changed) {
            location.setUpdatedBy(user != null ? user.getId() : null);
            location.setUpdatedAt(Instant.now());

            LocationEntity saved = locationRepository.save(location);

            saveLocationHistory(
                    saved,
                    oldValues,
                    newValues,
                    "UPDATED",
                    user
            );
        }

        return getLineCheckSettings(locationId);
    }


    // ---------------- HELPER METHODS ----------------
    private String buildFullAddress(LocationEntity location) {
        String street = Optional.ofNullable(location.getLocationStreet()).orElse("").trim();
        String town = Optional.ofNullable(location.getLocationTown()).orElse("").trim();
        String state = Optional.ofNullable(location.getLocationState()).orElse("").trim();
        String zip = Optional.ofNullable(location.getLocationZipCode()).orElse("").trim();

        if (!street.isEmpty()) return String.join(", ", street, town, state, zip, "USA").replaceAll(", ,", ",").replaceAll(", $", "");
        else if (!town.isEmpty() && !state.isEmpty()) return String.join(", ", town, state, "USA");
        else if (!zip.isEmpty()) return zip + ", USA";
        return "USA";
    }

    private void saveLocationHistory(LocationEntity location,
                                     Map<String, String> oldValues,
                                     Map<String, String> newValues,
                                     String changeType,
                                     UserEntity user) {
        try {
            LocationHistoryEntity history = LocationHistoryEntity.builder()
                    .location(location)
                    .locationName(location.getLocationName())
                    .changeType(changeType)
                    .changeAt(Instant.now())
                    .changedBy(user != null ? user.getId() : null)
                    .changedByName(user != null ? user.getUserName() : null)
                    .oldValues(oldValues != null ? objectMapper.writeValueAsString(oldValues) : "{}")
                    .newValues(newValues != null ? objectMapper.writeValueAsString(newValues) : "{}")
                    .build();

            locationHistoryRepository.save(history);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save location history", e);
        }
    }

    private Map<String, String> extractLocationFields(LocationEntity loc) {
        Map<String, String> values = new LinkedHashMap<>();

        values.put("locationName", loc.getLocationName());
        values.put("locationStreet", loc.getLocationStreet());
        values.put("locationTown", loc.getLocationTown());
        values.put("locationState", loc.getLocationState());
        values.put("locationZipCode", loc.getLocationZipCode());
        values.put("locationTimeZone", loc.getLocationTimeZone());

        values.put("locationLatitude",
                loc.getLocationLatitude() != null ? loc.getLocationLatitude().toString() : null);
        values.put("locationLongitude",
                loc.getLocationLongitude() != null ? loc.getLocationLongitude().toString() : null);

        values.put("locationActive",
                loc.getLocationActive() != null ? loc.getLocationActive().toString() : null);

        values.put("startOfWeek",
                loc.getStartOfWeek() != null ? loc.getStartOfWeek().name() : null);

        values.put("lineCheckDailyGoal",
                loc.getLineCheckDailyGoal() != null ? loc.getLineCheckDailyGoal().toString() : null);

        values.put("geocodedFromZipFallback",
                loc.getGeocodedFromZipFallback() != null
                        ? loc.getGeocodedFromZipFallback().toString()
                        : null);

        return values;
    }

}
