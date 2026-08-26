package com.backend.backend.repositories;

import com.backend.backend.entity.LineCheckCriterionResponseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LineCheckCriterionResponseRepository
        extends JpaRepository<LineCheckCriterionResponseEntity, UUID> {
}
