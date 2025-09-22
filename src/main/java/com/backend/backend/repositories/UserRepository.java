package com.backend.backend.repositories;

import com.backend.backend.entity.LocationEntity;
import com.backend.backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    @Query("SELECT u FROM UserEntity u WHERE u.accountId IS NULL OR u.id NOT IN (SELECT ua.user.id FROM UserAccessEntity ua)")
    List<UserEntity> findUsersWithoutAccountAccess();

    @Query("""
    SELECT ua.location FROM UserAccessEntity ua 
    WHERE ua.user.id = :userId 
      AND ua.account.id = :accountId 
      AND ua.location IS NOT NULL
      AND ua.location.account.id = ua.account.id
""")
    List<LocationEntity> findLocationsByUserIdAndAccountIdStrict(
            @Param("userId") UUID userId,
            @Param("accountId") UUID accountId
    );
}
