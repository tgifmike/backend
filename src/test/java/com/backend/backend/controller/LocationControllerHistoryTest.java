package com.backend.backend.controller;

import com.backend.backend.entity.LocationHistoryEntity;
import com.backend.backend.repositories.LocationHistoryRepository;
import com.backend.backend.service.LocationService;
import com.backend.backend.service.UserService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class LocationControllerHistoryTest {

    @Test
    void historyEndpointFiltersByLocationId() {
        UUID locationId = UUID.randomUUID();
        LocationHistoryEntity entry = new LocationHistoryEntity();
        AtomicReference<UUID> queriedLocationId = new AtomicReference<>();

        LocationHistoryRepository historyRepository = proxy(
                LocationHistoryRepository.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("findByLocation_IdOrderByChangeAtDesc")) {
                        queriedLocationId.set((UUID) args[0]);
                        return List.of(entry);
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
        LocationController controller = new LocationController(
                proxy(LocationService.class, unsupported()),
                proxy(UserService.class, unsupported()),
                historyRepository
        );

        List<LocationHistoryEntity> result = controller
                .getLocationHistory(locationId, null)
                .getBody();

        assertThat(queriedLocationId).hasValue(locationId);
        assertThat(result).containsExactly(entry);
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
