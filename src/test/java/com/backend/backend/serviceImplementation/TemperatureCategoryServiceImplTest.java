package com.backend.backend.serviceImplementation;

import com.backend.backend.config.UserContext;
import com.backend.backend.dto.ItemUpdateDto;
import com.backend.backend.dto.TemperatureCategoryCreateDto;
import com.backend.backend.dto.TemperatureCategoryDto;
import com.backend.backend.dto.TemperatureCategoryUpdateDto;
import com.backend.backend.entity.ItemEntity;
import com.backend.backend.entity.LocationEntity;
import com.backend.backend.entity.LocationHistoryEntity;
import com.backend.backend.entity.StationEntity;
import com.backend.backend.entity.TemperatureCategoryEntity;
import com.backend.backend.entity.UserEntity;
import com.backend.backend.enums.ItemTempCategory;
import com.backend.backend.repositories.ItemHistoryRepository;
import com.backend.backend.repositories.ItemRepository;
import com.backend.backend.repositories.LocationHistoryRepository;
import com.backend.backend.repositories.LocationRepository;
import com.backend.backend.repositories.StationRepository;
import com.backend.backend.repositories.TemperatureCategoryRepository;
import com.backend.backend.repositories.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemperatureCategoryServiceImplTest {

    @Test
    void restoreDefaultsResetsAllFourCategoriesAndWritesOneSummaryHistory() {
        UUID locationId = UUID.randomUUID();
        LocationEntity location = location(locationId);
        Map<UUID, TemperatureCategoryEntity> categories = new LinkedHashMap<>();

        TemperatureCategoryEntity customizedFrozen = category(
                UUID.randomUUID(),
                location,
                "FROZEN",
                -10.0,
                25.0
        );
        customizedFrozen.setName("My Freezer");
        categories.put(customizedFrozen.getId(), customizedFrozen);

        AtomicInteger inserts = new AtomicInteger();
        AtomicInteger thresholdUpdates = new AtomicInteger();
        List<LocationHistoryEntity> historyEntries = new ArrayList<>();
        TemperatureCategoryRepository categoryRepository = proxy(
                TemperatureCategoryRepository.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByLocation_IdAndCodeIgnoreCase" -> categories.values().stream()
                            .filter(category -> category.getCode().equalsIgnoreCase((String) args[1]))
                            .findFirst();
                    case "saveAndFlush" -> {
                        TemperatureCategoryEntity category = (TemperatureCategoryEntity) args[0];
                        if (category.getId() == null) {
                            category.setId(UUID.randomUUID());
                            inserts.incrementAndGet();
                        }
                        categories.put(category.getId(), category);
                        yield category;
                    }
                    case "findByLocation_IdOrderBySortOrderAscNameAsc" -> categories.values()
                            .stream()
                            .sorted(Comparator.comparing(
                                    TemperatureCategoryEntity::getSortOrder,
                                    Comparator.nullsLast(Integer::compareTo)
                            ))
                            .toList();
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );

        ItemRepository itemRepository = proxy(
                ItemRepository.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("updateTemperatureThresholds")) {
                        thresholdUpdates.incrementAndGet();
                        return 0;
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
        TemperatureCategoryServiceImpl service = new TemperatureCategoryServiceImpl(
                categoryRepository,
                locationRepository(location),
                itemRepository,
                historyRepository(historyEntries),
                userRepository(null),
                new ObjectMapper()
        );

        List<TemperatureCategoryDto> result = service.restoreDefaults(locationId);

        assertThat(result).hasSize(4);
        assertThat(inserts).hasValue(3);
        assertThat(thresholdUpdates).hasValue(4);
        assertThat(customizedFrozen.getName()).isEqualTo("Frozen");
        assertThat(customizedFrozen.getMinTemp()).isEqualTo(-20.0);
        assertThat(customizedFrozen.getMaxTemp()).isEqualTo(31.0);
        assertThat(customizedFrozen.getActive()).isTrue();

        assertThat(historyEntries).singleElement().satisfies(history -> {
            assertThat(history.getLocationId()).isEqualTo(locationId);
            assertThat(history.getLocationName()).isEqualTo("Test Location");
            assertThat(history.getEntityType()).isEqualTo("TEMPERATURE_CATEGORY");
            assertThat(history.getEntityId()).isNull();
            assertThat(history.getEntityName()).isEqualTo("Default temperature categories");
            assertThat(history.getChangeType()).isEqualTo("UPDATED");
            assertThat(history.getOldValues()).isEqualTo("{}");
            assertThat(readJson(history.getNewValues()).get("actionType").asText())
                    .isEqualTo("TEMPERATURE_DEFAULTS_RESTORED");
        });
    }

    @Test
    void changingThresholdsUpdatesItemsUsingTheCategory() {
        UUID locationId = UUID.randomUUID();
        LocationEntity location = location(locationId);
        TemperatureCategoryEntity category = category(
                UUID.randomUUID(),
                location,
                "REFRIGERATED",
                33.0,
                41.0
        );
        List<Object> propagatedValues = new ArrayList<>();
        List<LocationHistoryEntity> historyEntries = new ArrayList<>();
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setUserName("Manager User");

        TemperatureCategoryRepository categoryRepository = proxy(
                TemperatureCategoryRepository.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "findById" -> Optional.of(category);
                    case "existsByLocation_IdAndCodeIgnoreCaseAndIdNot" -> false;
                    case "saveAndFlush" -> args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
        ItemRepository itemRepository = proxy(
                ItemRepository.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("updateTemperatureThresholds")) {
                        propagatedValues.addAll(List.of(args));
                        return 2;
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
        TemperatureCategoryServiceImpl service = new TemperatureCategoryServiceImpl(
                categoryRepository,
                locationRepository(location),
                itemRepository,
                historyRepository(historyEntries),
                userRepository(user),
                new ObjectMapper()
        );
        TemperatureCategoryUpdateDto dto = new TemperatureCategoryUpdateDto(
                "REFRIGERATED",
                "Refrigerated",
                34.0,
                40.0,
                "F",
                1
        );

        UserContext.setCurrentUser(userId);
        try {
            service.update(category.getId(), dto);
        } finally {
            UserContext.clear();
        }

        assertThat(propagatedValues)
                .containsExactly(category.getId(), 34.0, 40.0);

        assertThat(historyEntries).singleElement().satisfies(history -> {
            assertThat(history.getLocation()).isSameAs(location);
            assertThat(history.getLocationId()).isEqualTo(locationId);
            assertThat(history.getLocationName()).isEqualTo("Test Location");
            assertThat(history.getEntityType()).isEqualTo("TEMPERATURE_CATEGORY");
            assertThat(history.getEntityId()).isEqualTo(category.getId());
            assertThat(history.getEntityName()).isEqualTo("Refrigerated");
            assertThat(history.getChangeType()).isEqualTo("UPDATED");
            assertThat(history.getChangedBy()).isEqualTo(userId);
            assertThat(history.getChangedByName()).isEqualTo("Manager User");

            JsonNode oldValues = readJson(history.getOldValues());
            JsonNode newValues = readJson(history.getNewValues());
            assertThat(oldValues.get("minTemp").asDouble()).isEqualTo(33.0);
            assertThat(oldValues.get("maxTemp").asDouble()).isEqualTo(41.0);
            assertThat(newValues.get("minTemp").asDouble()).isEqualTo(34.0);
            assertThat(newValues.get("maxTemp").asDouble()).isEqualTo(40.0);
            assertThat(newValues.has("active")).isFalse();
        });
    }

    @Test
    void deletingAnInUseCategoryReturnsConflict() {
        LocationEntity location = location(UUID.randomUUID());
        TemperatureCategoryEntity category = category(
                UUID.randomUUID(),
                location,
                "FROZEN",
                -20.0,
                31.0
        );
        TemperatureCategoryRepository categoryRepository = proxy(
                TemperatureCategoryRepository.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("findById")) {
                        return Optional.of(category);
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
        ItemRepository itemRepository = proxy(
                ItemRepository.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("existsAnyByTemperatureCategoryId")) {
                        return true;
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
        TemperatureCategoryServiceImpl service = categoryService(
                categoryRepository,
                locationRepository(location),
                itemRepository
        );

        assertThatThrownBy(() -> service.delete(category.getId()))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex ->
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT)
                );
    }

    @Test
    void rejectsAnInvalidTemperatureRange() {
        TemperatureCategoryServiceImpl service = categoryService(
                proxy(TemperatureCategoryRepository.class, unsupported()),
                proxy(LocationRepository.class, unsupported()),
                proxy(ItemRepository.class, unsupported())
        );
        TemperatureCategoryCreateDto dto = new TemperatureCategoryCreateDto(
                UUID.randomUUID(),
                "INVALID",
                "Invalid",
                50.0,
                40.0,
                "F",
                true,
                0
        );

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getReason()).isEqualTo("minTemp must be less than maxTemp");
                });
    }

    @Test
    void itemUpdateCalculatesTemperatureChangeBeforeMutatingTheFlag() {
        UUID locationId = UUID.randomUUID();
        LocationEntity location = location(locationId);
        StationEntity station = new StationEntity();
        station.setId(UUID.randomUUID());
        station.setLocation(location);

        ItemEntity item = new ItemEntity();
        item.setId(UUID.randomUUID());
        item.setStation(station);
        item.setIsTempTaken(false);

        TemperatureCategoryEntity category = category(
                UUID.randomUUID(),
                location,
                "REFRIGERATED",
                33.0,
                41.0
        );

        ItemRepository itemRepository = proxy(
                ItemRepository.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "findById" -> Optional.of(item);
                    case "save" -> args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
        TemperatureCategoryRepository categoryRepository = proxy(
                TemperatureCategoryRepository.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("findById")) {
                        return Optional.of(category);
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
        UserRepository userRepository = proxy(
                UserRepository.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("findById")) {
                        return Optional.empty();
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
        ItemHistoryRepository historyRepository = proxy(
                ItemHistoryRepository.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("save")) {
                        return args[0];
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );

        ItemServiceImpl itemService = new ItemServiceImpl(
                itemRepository,
                proxy(StationRepository.class, unsupported()),
                historyRepository,
                userRepository,
                categoryRepository
        );
        ItemUpdateDto dto = new ItemUpdateDto();
        dto.setIsTempTaken(true);
        dto.setTempCategoryId(category.getId());

        ItemEntity updated = itemService.updateItem(
                station.getId(),
                item.getId(),
                dto,
                UUID.randomUUID()
        );

        assertThat(updated.getIsTempTaken()).isTrue();
        assertThat(updated.getTemperatureCategory()).isSameAs(category);
        assertThat(updated.getTempCategory()).isEqualTo(ItemTempCategory.REFRIGERATED);
        assertThat(updated.getMinTemp()).isEqualTo(33.0);
        assertThat(updated.getMaxTemp()).isEqualTo(41.0);
    }

    private static LocationRepository locationRepository(LocationEntity location) {
        return proxy(
                LocationRepository.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("findById")) {
                        return Optional.of(location);
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static LocationEntity location(UUID id) {
        LocationEntity location = new LocationEntity();
        location.setId(id);
        location.setLocationName("Test Location");
        return location;
    }

    private static TemperatureCategoryServiceImpl categoryService(
            TemperatureCategoryRepository categoryRepository,
            LocationRepository locationRepository,
            ItemRepository itemRepository
    ) {
        return new TemperatureCategoryServiceImpl(
                categoryRepository,
                locationRepository,
                itemRepository,
                historyRepository(new ArrayList<>()),
                userRepository(null),
                new ObjectMapper()
        );
    }

    private static LocationHistoryRepository historyRepository(
            List<LocationHistoryEntity> entries
    ) {
        return proxy(
                LocationHistoryRepository.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("save")) {
                        LocationHistoryEntity history = (LocationHistoryEntity) args[0];
                        entries.add(history);
                        return history;
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static UserRepository userRepository(UserEntity user) {
        return proxy(
                UserRepository.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("findById")) {
                        return Optional.ofNullable(user);
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static JsonNode readJson(String json) {
        try {
            return new ObjectMapper().readTree(json);
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    private static TemperatureCategoryEntity category(
            UUID id,
            LocationEntity location,
            String code,
            Double minTemp,
            Double maxTemp
    ) {
        return TemperatureCategoryEntity.builder()
                .id(id)
                .location(location)
                .code(code)
                .name(code)
                .minTemp(minTemp)
                .maxTemp(maxTemp)
                .unit("F")
                .active(true)
                .systemDefault(false)
                .sortOrder(0)
                .build();
    }

    private static InvocationHandler unsupported() {
        return (proxy, method, args) -> {
            throw new UnsupportedOperationException(method.getName());
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                handler
        );
    }
}
