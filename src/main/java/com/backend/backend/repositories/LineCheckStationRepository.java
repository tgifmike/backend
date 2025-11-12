package com.backend.backend.repositories;


import com.backend.backend.entity.LineCheckStationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LineCheckStationRepository extends JpaRepository<LineCheckStationEntity, UUID> {}

