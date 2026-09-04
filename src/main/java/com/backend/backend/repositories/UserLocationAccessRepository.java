package com.backend.backend.repositories;

import com.backend.backend.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserLocationAccessRepository extends JpaRepository<UserLocationAccessEntity, UUID> {
    List<UserLocationAccessEntity> findByUser(UserEntity user);
    List<UserLocationAccessEntity> findByUserId(UUID userId);
    List<UserLocationAccessEntity> findByLocation(LocationEntity location);
    boolean existsByUserAndLocation(UserEntity user, LocationEntity location);
    boolean existsByUserIdAndLocationId(UUID userId, UUID locationId);

    @Query("select a.location from UserLocationAccessEntity a where a.user.id = :userId and a.location.account.id = :accountId and a.location.deletedAt is null")
    List<LocationEntity> findLocationsByUserIdAndAccountId(@Param("userId") UUID userId, @Param("accountId") UUID accountId);

//    @Query("SELECT ua.account.id FROM UserAccountAccess ua WHERE ua.user.id = :userId")
//    List<UUID> findAccountIdsByUserId(@Param("userId") UUID userId);
}
