package com.backend.backend.repositories;

import com.backend.backend.entity.LocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LocationRepository extends JpaRepository<LocationEntity, UUID> {

    // Find by location name (global – use sparingly)
    Optional<LocationEntity> findByLocationName(String locationName);

    // Check existence by name within an account
    boolean existsByLocationNameAndAccount_Id(String locationName, UUID accountId);
    boolean existsByLocationNameIgnoreCaseAndAccount_Id(String locationName, UUID accountId);


    // Find all locations for an account (active only due to @Where)
    List<LocationEntity> findByAccount_Id(UUID accountId);

    // All locations ordered by creation date
    @Query(
            value = "SELECT * FROM locations WHERE deleted_at IS NULL ORDER BY created_at ASC",
            nativeQuery = true
    )
    List<LocationEntity> findAllOrderedByCreatedAt();

    // Fetch including soft-deleted locations
    @Query(
            value = "SELECT * FROM locations ORDER BY created_at ASC",
            nativeQuery = true
    )
    List<LocationEntity> findAllIncludingDeleted();

    // Fetch locations accessible to a user (via user_location_access table)
    @Query(
            value = "SELECT l.* FROM locations l " +
                    "JOIN user_location_access ula ON l.id = ula.location_id " +
                    "WHERE ula.user_id = :userId " +
                    "AND l.deleted_at IS NULL " +
                    "ORDER BY l.created_at ASC",
            nativeQuery = true
    )
    List<LocationEntity> findActiveLocationsByUserId(UUID userId);

    // Fetch locations by multiple account IDs (active only)
    List<LocationEntity> findByAccount_IdIn(List<UUID> accountIds);

    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END " +
            "FROM LocationEntity l " +
            "WHERE l.account.id = :accountId AND LOWER(l.locationName) = LOWER(:name) " +
            "AND l.deletedAt IS NULL")
    boolean existsActiveByLocationNameIgnoreCaseAndAccountId(@Param("name") String name, @Param("accountId") UUID accountId);

}

