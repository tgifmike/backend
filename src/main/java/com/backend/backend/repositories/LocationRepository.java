package com.backend.backend.repositories;

import com.backend.backend.entity.LocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocationRepository extends JpaRepository<LocationEntity, UUID> {
    Optional<LocationEntity> findByLocationName(String locationName);
    List<LocationEntity> findByAccountId(UUID accountId);
    boolean existsByLocationName(String locationName);
}
