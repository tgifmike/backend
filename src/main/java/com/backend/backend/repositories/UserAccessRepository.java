package com.backend.backend.repositories;

import com.backend.backend.entity.AccountEntity;
import com.backend.backend.entity.LocationEntity;
import com.backend.backend.entity.UserAccessEntity;
import com.backend.backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAccessRepository extends JpaRepository<UserAccessEntity, UUID> {

    List<UserAccessEntity> findByUserId(UUID userId);
    boolean existsByUserIdAndLocationId(UUID userId, UUID locationId);
    void deleteByUserIdAndLocationId(UUID userId, UUID locationId);



    @Query("SELECT DISTINCT ua.account FROM UserAccessEntity ua WHERE ua.user.id = :userId")
    List<AccountEntity> findAccountsByUserId(@Param("userId") UUID userId);

    boolean existsByUserIdAndAccountIdAndLocationIsNull(UUID userId, UUID accountId);

    @Query("SELECT ua FROM UserAccessEntity ua WHERE ua.user.id = :userId AND ua.account.id = :accountId AND ua.location IS NULL")
    UserAccessEntity findAccountLevelAccess(@Param("userId") UUID userId, @Param("accountId") UUID accountId);

    @Query("SELECT ua FROM UserAccessEntity ua WHERE ua.user.id = :userId AND ua.location.id = :locationId")
    UserAccessEntity findByUserAndLocation(@Param("userId") UUID userId, @Param("locationId") UUID locationId);

    @Query("SELECT DISTINCT ua.user FROM UserAccessEntity ua WHERE ua.account.id = :accountId")
    List<UserEntity> findUsersByAccountId(@Param("accountId") UUID accountId);

    @Query("""
    SELECT ua.location FROM UserAccessEntity ua 
    WHERE ua.user.id = :userId 
      AND ua.account.id = :accountId 
      AND ua.location IS NOT NULL
""")
    List<LocationEntity> findLocationsByUserIdAndAccountId(
            @Param("userId") UUID userId,
            @Param("accountId") UUID accountId
    );

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

    @Query("SELECT ua FROM UserAccessEntity ua WHERE ua.user.id = :userId AND ua.account.id = :accountId AND ua.location IS NULL")
    Optional<UserAccessEntity> findAccountLevelAccessOptional(@Param("userId") UUID userId, @Param("accountId") UUID accountId);


    @Modifying(clearAutomatically = true)          // bulk write
    @Transactional                                 // start/commit tx here
    @Query("DELETE FROM UserAccessEntity ua WHERE ua.location.id = :locationId")
    void deleteByLocationId(@Param("locationId") UUID locationId);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserAccessEntity ua WHERE ua.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM UserAccessEntity ua WHERE ua.user.id = :userId AND ua.account.id = :accountId AND ua.location.id = :locationId")
    void deleteByUserIdAndAccountIdAndLocationId(
            @Param("userId") UUID userId,
            @Param("accountId") UUID accountId,
            @Param("locationId") UUID locationId
    );



}
