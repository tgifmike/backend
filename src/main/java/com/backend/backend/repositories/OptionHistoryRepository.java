package com.backend.backend.repositories;

import com.backend.backend.entity.OptionHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OptionHistoryRepository extends JpaRepository<OptionHistoryEntity, UUID> {

    List<OptionHistoryEntity> findByAccountIdOrderByChangeAtDesc(UUID accountId);

    List<OptionHistoryEntity> findByOptionIdOrderByChangeAtDesc(UUID optionId);
}
