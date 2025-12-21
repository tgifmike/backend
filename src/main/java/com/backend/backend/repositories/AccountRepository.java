package com.backend.backend.repositories;

import com.backend.backend.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {

    // Find by account name
    Optional<AccountEntity> findByAccountName(String accountName);

    // Check existence by account name
    boolean existsByAccountName(String accountName);

    // All accounts ordered by creation date
    @Query(
            value = "SELECT * FROM accounts ORDER BY created_at ASC",
            nativeQuery = true
    )
    List<AccountEntity> findAllOrderedByCreatedAt();

    // Fetch including "soft deleted" accounts if you have a deleted flag
    @Query(
            value = "SELECT * FROM accounts /* WHERE deleted_at IS NOT NULL */ ORDER BY created_at ASC",
            nativeQuery = true
    )
    List<AccountEntity> findAllIncludingDeleted();

    // Optional: find all accounts by user ID if you have user-account access table
    @Query(
            value = "SELECT a.* FROM accounts a " +
                    "JOIN user_account_access uaa ON a.id = uaa.account_id " +
                    "WHERE uaa.user_id = :userId " +
                    "AND a.deleted_at IS NULL " +
                    "ORDER BY a.created_at ASC",
            nativeQuery = true
    )
    List<AccountEntity> findActiveAccountsByUserId(UUID userId);


}
