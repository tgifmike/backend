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

    List<LocationEntity> findByAccountId(UUID accountId);
    Optional<LocationEntity> findByAccount_NameAndName(String accountName, String name);
    boolean existsByAccountIdAndName(UUID accountId, String name);

    @Query("SELECT l FROM LocationEntity l JOIN l.account a WHERE a.name = :accountName AND l.id = :locationId")
    Optional<LocationEntity> findByAccountNameAndId(@Param("accountName") String accountName, @Param("locationId") UUID locationId);

    @Query("SELECT l FROM UserAccessEntity ua JOIN ua.location l WHERE ua.user.id = :userId AND ua.account.id = :accountId")
    List<LocationEntity> findLocationsByUserIdAndAccountId(@Param("userId") UUID userId, @Param("accountId") UUID accountId);
}
