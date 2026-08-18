package com.backend.backend.repositories;

import com.backend.backend.entity.LineCheckPhotoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LineCheckPhotoRepository
        extends JpaRepository<LineCheckPhotoEntity, UUID> {

    List<LineCheckPhotoEntity> findByLineCheckItemId(UUID lineCheckItemId);
}
