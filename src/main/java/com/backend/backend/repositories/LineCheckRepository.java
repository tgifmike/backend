package com.backend.backend.repositories;

import com.backend.backend.entity.LineCheckEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LineCheckRepository extends JpaRepository<LineCheckEntity, UUID> {
    List<LineCheckEntity> findAllByOrderByCheckTimeDesc();
}

