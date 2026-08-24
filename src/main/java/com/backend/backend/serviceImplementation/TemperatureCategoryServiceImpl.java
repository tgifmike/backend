package com.backend.backend.serviceImplementation;

import com.backend.backend.config.UserContext;
import com.backend.backend.dto.TemperatureCategoryCreateDto;
import com.backend.backend.dto.TemperatureCategoryDto;
import com.backend.backend.dto.TemperatureCategoryUpdateDto;
import com.backend.backend.entity.LocationEntity;
import com.backend.backend.entity.LocationHistoryEntity;
import com.backend.backend.entity.TemperatureCategoryEntity;
import com.backend.backend.repositories.ItemRepository;
import com.backend.backend.repositories.LocationHistoryRepository;
import com.backend.backend.repositories.LocationRepository;
import com.backend.backend.repositories.TemperatureCategoryRepository;
import com.backend.backend.repositories.UserRepository;
import com.backend.backend.service.TemperatureCategoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TemperatureCategoryServiceImpl implements TemperatureCategoryService {

    private static final List<DefaultCategory> DEFAULT_CATEGORIES = List.of(
            new DefaultCategory("FROZEN", "Frozen", -20.0, 31.0, 0),
            new DefaultCategory("REFRIGERATED", "Refrigerated", 33.0, 41.0, 1),
            new DefaultCategory("ROOM_TEMP", "Room Temperature", 50.0, 75.0, 2),
            new DefaultCategory("HOT_HOLDING", "Hot Holding", 140.0, 200.0, 3)
    );

    private final TemperatureCategoryRepository temperatureCategoryRepository;
    private final LocationRepository locationRepository;
    private final ItemRepository itemRepository;
    private final LocationHistoryRepository locationHistoryRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<TemperatureCategoryDto> getByLocation(UUID locationId) {
        requireLocation(locationId);
        return temperatureCategoryRepository
                .findByLocation_IdOrderBySortOrderAscNameAsc(locationId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public TemperatureCategoryDto create(TemperatureCategoryCreateDto dto) {
        validateRange(dto.getMinTemp(), dto.getMaxTemp());

        LocationEntity location = requireLocation(dto.getLocationId());
        String code = normalizeCode(dto.getCode());
        rejectDuplicateCode(location.getId(), code, null);

        TemperatureCategoryEntity category = TemperatureCategoryEntity.builder()
                .location(location)
                .code(code)
                .name(dto.getName())
                .minTemp(dto.getMinTemp())
                .maxTemp(dto.getMaxTemp())
                .unit(dto.getUnit())
                .active(dto.getActive() == null || dto.getActive())
                .systemDefault(false)
                .sortOrder(dto.getSortOrder())
                .build();

        TemperatureCategoryEntity saved = save(category);
        saveHistory(saved, "CREATED", Map.of(), snapshot(saved));
        return toDto(saved);
    }

    @Override
    @Transactional
    public TemperatureCategoryDto update(
            UUID categoryId,
            TemperatureCategoryUpdateDto dto
    ) {
        validateRange(dto.getMinTemp(), dto.getMaxTemp());

        TemperatureCategoryEntity category = requireCategory(categoryId);
        String code = normalizeCode(dto.getCode());
        rejectDuplicateCode(category.getLocation().getId(), code, categoryId);

        String name = dto.getName().trim();
        String unit = normalizeUnit(dto.getUnit());
        Map<String, Object> oldValues = new LinkedHashMap<>();
        Map<String, Object> newValues = new LinkedHashMap<>();

        trackChange("code", category.getCode(), code, oldValues, newValues);
        trackChange("name", category.getName(), name, oldValues, newValues);
        trackChange("minTemp", category.getMinTemp(), dto.getMinTemp(), oldValues, newValues);
        trackChange("maxTemp", category.getMaxTemp(), dto.getMaxTemp(), oldValues, newValues);
        trackChange("unit", category.getUnit(), unit, oldValues, newValues);
        trackChange("sortOrder", category.getSortOrder(), dto.getSortOrder(), oldValues, newValues);

        boolean thresholdsChanged =
                !category.getMinTemp().equals(dto.getMinTemp())
                        || !category.getMaxTemp().equals(dto.getMaxTemp());

        category.setCode(code);
        category.setName(name);
        category.setMinTemp(dto.getMinTemp());
        category.setMaxTemp(dto.getMaxTemp());
        category.setUnit(unit);
        category.setSortOrder(dto.getSortOrder());

        TemperatureCategoryEntity saved = save(category);

        if (thresholdsChanged) {
            itemRepository.updateTemperatureThresholds(
                    saved.getId(),
                    saved.getMinTemp(),
                    saved.getMaxTemp()
            );
        }

        if (!oldValues.isEmpty()) {
            saveHistory(saved, "UPDATED", oldValues, newValues);
        }

        return toDto(saved);
    }

    @Override
    @Transactional
    public TemperatureCategoryDto setActive(UUID categoryId, boolean active) {
        TemperatureCategoryEntity category = requireCategory(categoryId);

        if (Objects.equals(category.getActive(), active)) {
            return toDto(category);
        }

        boolean oldActive = Boolean.TRUE.equals(category.getActive());
        category.setActive(active);
        TemperatureCategoryEntity saved = save(category);
        saveHistory(
                saved,
                "UPDATED",
                Map.of("active", oldActive),
                Map.of("active", active)
        );
        return toDto(saved);
    }

    @Override
    @Transactional
    public void delete(UUID categoryId) {
        TemperatureCategoryEntity category = requireCategory(categoryId);

        if (itemRepository.existsAnyByTemperatureCategoryId(categoryId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Temperature category is used by an item; deactivate it instead"
            );
        }

        Map<String, Object> oldValues = snapshot(category);
        temperatureCategoryRepository.delete(category);
        saveHistory(category, "DELETED", oldValues, Map.of());
    }

    @Override
    @Transactional
    public List<TemperatureCategoryDto> restoreDefaults(UUID locationId) {
        LocationEntity location = requireLocation(locationId);

        for (DefaultCategory defaultCategory : DEFAULT_CATEGORIES) {
            TemperatureCategoryEntity category = temperatureCategoryRepository
                    .findByLocation_IdAndCodeIgnoreCase(
                            locationId,
                            defaultCategory.code()
                    )
                    .orElseGet(() -> TemperatureCategoryEntity.builder()
                            .location(location)
                            .code(defaultCategory.code())
                            .build());

            category.setName(defaultCategory.name());
            category.setMinTemp(defaultCategory.minTemp());
            category.setMaxTemp(defaultCategory.maxTemp());
            category.setUnit("F");
            category.setActive(true);
            category.setSystemDefault(true);
            category.setSortOrder(defaultCategory.sortOrder());

            TemperatureCategoryEntity saved = save(category);
            itemRepository.updateTemperatureThresholds(
                    saved.getId(),
                    saved.getMinTemp(),
                    saved.getMaxTemp()
            );
        }

        saveDefaultsRestoredHistory(location);

        return temperatureCategoryRepository
                .findByLocation_IdOrderBySortOrderAscNameAsc(locationId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private LocationEntity requireLocation(UUID locationId) {
        return locationRepository.findById(locationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Location not found"
                ));
    }

    private TemperatureCategoryEntity requireCategory(UUID categoryId) {
        return temperatureCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Temperature category not found"
                ));
    }

    private void rejectDuplicateCode(UUID locationId, String code, UUID categoryId) {
        boolean duplicate = categoryId == null
                ? temperatureCategoryRepository.existsByLocation_IdAndCodeIgnoreCase(
                        locationId,
                        code
                )
                : temperatureCategoryRepository.existsByLocation_IdAndCodeIgnoreCaseAndIdNot(
                        locationId,
                        code,
                        categoryId
                );

        if (duplicate) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Temperature category code already exists for this location"
            );
        }
    }

    private TemperatureCategoryEntity save(TemperatureCategoryEntity category) {
        try {
            return temperatureCategoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Temperature category code already exists for this location",
                    ex
            );
        }
    }

    private void validateRange(Double minTemp, Double maxTemp) {
        if (minTemp == null || maxTemp == null || minTemp >= maxTemp) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "minTemp must be less than maxTemp"
            );
        }
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeUnit(String unit) {
        return unit == null || unit.isBlank()
                ? "F"
                : unit.trim().toUpperCase(Locale.ROOT);
    }

    private void trackChange(
            String field,
            Object oldValue,
            Object newValue,
            Map<String, Object> oldValues,
            Map<String, Object> newValues
    ) {
        if (!Objects.equals(oldValue, newValue)) {
            oldValues.put(field, oldValue);
            newValues.put(field, newValue);
        }
    }

    private Map<String, Object> snapshot(TemperatureCategoryEntity category) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("categoryId", category.getId());
        values.put("code", category.getCode());
        values.put("name", category.getName());
        values.put("minTemp", category.getMinTemp());
        values.put("maxTemp", category.getMaxTemp());
        values.put("unit", category.getUnit());
        values.put("active", category.getActive());
        values.put("systemDefault", category.getSystemDefault());
        values.put("sortOrder", category.getSortOrder());
        return values;
    }

    private void saveHistory(
            TemperatureCategoryEntity category,
            String changeType,
            Map<String, Object> oldValues,
            Map<String, Object> newValues
    ) {
        HistoryActor actor = currentActor();

        LocationHistoryEntity history = LocationHistoryEntity.builder()
                .location(category.getLocation())
                .locationName(category.getLocation().getLocationName())
                .entityType("TEMPERATURE_CATEGORY")
                .entityId(category.getId())
                .entityName(category.getName())
                .changeType(changeType)
                .changeAt(Instant.now())
                .changedBy(actor.userId())
                .changedByName(actor.userName())
                .oldValues(toJson(oldValues))
                .newValues(toJson(newValues))
                .build();

        locationHistoryRepository.save(history);
    }

    private void saveDefaultsRestoredHistory(LocationEntity location) {
        HistoryActor actor = currentActor();

        LocationHistoryEntity history = LocationHistoryEntity.builder()
                .location(location)
                .locationName(location.getLocationName())
                .entityType("TEMPERATURE_CATEGORY")
                .entityName("Default temperature categories")
                .changeType("UPDATED")
                .changeAt(Instant.now())
                .changedBy(actor.userId())
                .changedByName(actor.userName())
                .oldValues("{}")
                .newValues(toJson(Map.of(
                        "actionType",
                        "TEMPERATURE_DEFAULTS_RESTORED"
                )))
                .build();

        locationHistoryRepository.save(history);
    }

    private HistoryActor currentActor() {
        UUID userId = UserContext.getCurrentUser();
        String userName = userId == null
                ? "System"
                : userRepository.findById(userId)
                        .map(user -> user.getUserName() != null
                                ? user.getUserName()
                                : user.getId().toString())
                        .orElse("Unknown User");

        return new HistoryActor(userId, userName);
    }

    private String toJson(Map<String, Object> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize location history", ex);
        }
    }

    private TemperatureCategoryDto toDto(TemperatureCategoryEntity category) {
        return TemperatureCategoryDto.builder()
                .id(category.getId())
                .locationId(category.getLocation().getId())
                .code(category.getCode())
                .name(category.getName())
                .minTemp(category.getMinTemp())
                .maxTemp(category.getMaxTemp())
                .unit(category.getUnit())
                .active(category.getActive())
                .systemDefault(category.getSystemDefault())
                .sortOrder(category.getSortOrder())
                .build();
    }

    private record DefaultCategory(
            String code,
            String name,
            Double minTemp,
            Double maxTemp,
            Integer sortOrder
    ) {
    }

    private record HistoryActor(UUID userId, String userName) {
    }
}
