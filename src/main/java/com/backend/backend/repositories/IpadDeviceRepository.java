package com.backend.backend.repositories;

import com.backend.backend.entity.IpadDeviceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface IpadDeviceRepository extends JpaRepository<IpadDeviceEntity, UUID> {
    @Query("select d from IpadDeviceEntity d join fetch d.account a left join fetch d.location l where a.id = :accountId order by d.enrolledAt desc")
    List<IpadDeviceEntity> findAllByAccountId(@Param("accountId") UUID accountId);
    @Query("""
            SELECT d FROM IpadDeviceEntity d
            JOIN FETCH d.account
            LEFT JOIN FETCH d.location
            WHERE d.id = :id AND d.active = true
            """)
    Optional<IpadDeviceEntity> findByIdAndActiveTrueWithScope(@Param("id") UUID id);

    Optional<IpadDeviceEntity> findByIdAndActiveTrue(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT d FROM IpadDeviceEntity d
            JOIN FETCH d.account
            LEFT JOIN FETCH d.location
            WHERE d.id = :id
            """)
    Optional<IpadDeviceEntity> findByIdForUpdate(@Param("id") UUID id);
}
