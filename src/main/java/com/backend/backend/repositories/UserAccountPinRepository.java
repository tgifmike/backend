package com.backend.backend.repositories;

import com.backend.backend.entity.UserAccountPinEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAccountPinRepository extends JpaRepository<UserAccountPinEntity, UUID> {
    Optional<UserAccountPinEntity> findByAccountIdAndUserId(UUID accountId, UUID userId);

    Optional<UserAccountPinEntity> findByAccountIdAndPinLookupDigest(UUID accountId, String pinLookupDigest);

    boolean existsByAccountIdAndPinLookupDigest(UUID accountId, String pinLookupDigest);

    @Query("""
            SELECT DISTINCT p FROM UserAccountPinEntity p
            JOIN FETCH p.user u
            WHERE p.account.id = :accountId
              AND p.status = com.backend.backend.enums.PinCredentialStatus.ACTIVE
              AND u.userActive = true
              AND u.deletedAt IS NULL
            """)
    List<UserAccountPinEntity> findAllActiveByAccountId(@Param("accountId") UUID accountId);

    @Query("""
            SELECT p FROM UserAccountPinEntity p
            JOIN FETCH p.user
            WHERE p.account.id = :accountId
            """)
    List<UserAccountPinEntity> findAllByAccountId(@Param("accountId") UUID accountId);

    @Query("""
            SELECT DISTINCT p FROM UserAccountPinEntity p
            JOIN FETCH p.user u
            JOIN UserAccountAccessEntity uaa ON uaa.user.id = u.id
            JOIN UserLocationAccessEntity ula ON ula.user.id = u.id
            WHERE p.account.id = :accountId
              AND uaa.account.id = :accountId
              AND ula.location.id = :locationId
              AND p.status = com.backend.backend.enums.PinCredentialStatus.ACTIVE
              AND u.userActive = true
              AND u.deletedAt IS NULL
            """)
    List<UserAccountPinEntity> findActiveForLocation(
            @Param("accountId") UUID accountId,
            @Param("locationId") UUID locationId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p FROM UserAccountPinEntity p
            JOIN FETCH p.user
            JOIN FETCH p.account
            WHERE p.account.id = :accountId AND p.user.id = :userId
            """)
    Optional<UserAccountPinEntity> findByAccountIdAndUserIdForUpdate(
            @Param("accountId") UUID accountId,
            @Param("userId") UUID userId
    );
}
