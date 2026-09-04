package com.backend.backend.repositories;

import com.backend.backend.entity.PinAuthenticationAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PinAuthenticationAuditRepository extends JpaRepository<PinAuthenticationAuditEntity, UUID> {
    boolean existsBySourceEventId(UUID sourceEventId);
}
