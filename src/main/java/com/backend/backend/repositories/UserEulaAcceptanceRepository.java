package com.backend.backend.repositories;

import com.backend.backend.entity.UserEulaAcceptanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserEulaAcceptanceRepository extends JpaRepository<UserEulaAcceptanceEntity, UUID> {
    Optional<UserEulaAcceptanceEntity> findTopByUserIdOrderByEulaVersionDesc(UUID userId);
    boolean existsByUserIdAndEulaVersion(UUID userId, Integer eulaVersion);
}
