package com.backend.backend.repositories;

import com.backend.backend.entity.OptionEntity;
import com.backend.backend.config.OptionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OptionRepository extends JpaRepository<OptionEntity, UUID> {

    // All options for an account, sorted by sortOrder
    List<OptionEntity> findByAccountIdOrderBySortOrderAsc(UUID accountId);

    // Active options only, sorted
    List<OptionEntity> findByAccountIdAndOptionActiveTrueOrderBySortOrderAsc(UUID accountId);

    // Optional: find by option type, sorted
    List<OptionEntity> findByAccountIdAndOptionTypeOrderBySortOrderAsc(UUID accountId, OptionType optionType);

    List<OptionEntity> findByAccountIdAndOptionTypeOrderBySortOrder(
            UUID accountId,
            OptionType optionType
    );


    // Fetch all options for an account, ordered by sortOrder
    List<OptionEntity> findByAccountIdOrderBySortOrder(UUID accountId);
}

