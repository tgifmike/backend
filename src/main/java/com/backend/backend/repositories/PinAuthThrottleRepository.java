package com.backend.backend.repositories;

import com.backend.backend.entity.PinAuthThrottleEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PinAuthThrottleRepository extends JpaRepository<PinAuthThrottleEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT t FROM PinAuthThrottleEntity t
            WHERE t.scopeType = :scopeType
              AND t.accountId = :accountId
              AND t.ipAddress = :ipAddress
            """)
    Optional<PinAuthThrottleEntity> findForUpdate(
            @Param("scopeType") String scopeType,
            @Param("accountId") UUID accountId,
            @Param("ipAddress") String ipAddress
    );
}
