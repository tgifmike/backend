package com.backend.backend.repositories;

import com.backend.backend.entity.OptionEntity;
import com.backend.backend.enums.OptionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

//@Repository
//public interface OptionRepository extends JpaRepository<OptionEntity, UUID> {
//
//    // All options for an account, sorted by sortOrder
//    List<OptionEntity> findByAccountIdOrderBySortOrderAsc(UUID accountId);
//
//    // Active options only, sorted
//    List<OptionEntity> findByAccountIdAndOptionActiveTrueOrderBySortOrderAsc(UUID accountId);
//
//    // Optional: find by option type, sorted
//   // List<OptionEntity> findByAccountIdAndOptionTypeOrderBySortOrderAsc(UUID accountId, OptionType optionType);
//
////    List<OptionEntity> findByAccountIdAndOptionTypeOrderBySortOrder(
////            UUID accountId,
////            OptionType optionType
////    );
//
//
//    // Fetch all options for an account, ordered by sortOrder
//    List<OptionEntity> findByAccountIdOrderBySortOrder(UUID accountId);
//}

//@Repository
//public interface OptionRepository extends JpaRepository<OptionEntity, UUID> {
//
//    List<OptionEntity> findByAccountIdOrderBySortOrderAsc(UUID accountId);
//
//    List<OptionEntity> findByAccountIdAndOptionActiveTrueOrderBySortOrderAsc(UUID accountId);
//
//    // REQUIRED for optionType filters
//    List<OptionEntity> findByAccountIdAndOptionTypeOrderBySortOrderAsc(
//            UUID accountId,
//            OptionType optionType
//    );
//}
@Repository
public interface OptionRepository extends JpaRepository<OptionEntity, UUID> {

    // All options for an account (non-deleted automatically via @Where)
    List<OptionEntity> findByAccountIdOrderBySortOrderAsc(UUID accountId);

    // Options filtered by type
    List<OptionEntity> findByAccountIdAndOptionTypeOrderBySortOrderAsc(UUID accountId, OptionType optionType);

    // Only active options
    List<OptionEntity> findByAccountIdAndOptionActiveTrueOrderBySortOrderAsc(UUID accountId);

    //for logs
//    @Query("SELECT o FROM OptionEntity o WHERE o.account.id = :accountId")
//    List<OptionEntity> findAllByAccountIdIncludingDeleted(@Param("accountId") UUID accountId);


    @Query(
            value = "SELECT * FROM options WHERE account_id = :accountId ORDER BY created_at ASC",
            nativeQuery = true
    )
    List<OptionEntity> findAllByAccountIdIncludingDeleted(UUID accountId);

}


