package com.backend.backend.repositories;

import com.backend.backend.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserLocationAccessRepository extends JpaRepository<UserLocationAccessEntity, UUID> {
    List<UserLocationAccessEntity> findByUser(UserEntity user);
    List<UserLocationAccessEntity> findByLocation(LocationEntity location);
    boolean existsByUserAndLocation(UserEntity user, LocationEntity location);
    boolean existsByUserIdAndLocationId(UUID userId, UUID locationId);

//    @Query("SELECT ua.account.id FROM UserAccountAccess ua WHERE ua.user.id = :userId")
//    List<UUID> findAccountIdsByUserId(@Param("userId") UUID userId);
}