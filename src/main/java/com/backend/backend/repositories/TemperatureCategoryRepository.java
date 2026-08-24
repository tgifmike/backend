package com.backend.backend.repositories;

import com.backend.backend.entity.TemperatureCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TemperatureCategoryRepository
        extends JpaRepository<TemperatureCategoryEntity, UUID> {

    List<TemperatureCategoryEntity> findByLocation_IdOrderBySortOrderAscNameAsc(UUID locationId);

    Optional<TemperatureCategoryEntity> findByLocation_IdAndCodeIgnoreCase(
            UUID locationId,
            String code
    );

    boolean existsByLocation_IdAndCodeIgnoreCase(UUID locationId, String code);

    boolean existsByLocation_IdAndCodeIgnoreCaseAndIdNot(
            UUID locationId,
            String code,
            UUID id
    );
}
