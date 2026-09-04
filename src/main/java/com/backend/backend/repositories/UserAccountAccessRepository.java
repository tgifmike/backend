package com.backend.backend.repositories;

import com.backend.backend.entity.AccountEntity;
import com.backend.backend.entity.UserAccountAccessEntity;
import com.backend.backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserAccountAccessRepository extends JpaRepository<UserAccountAccessEntity, UUID> {
    List<UserAccountAccessEntity> findByUser(UserEntity user);
    @Query("SELECT access FROM UserAccountAccessEntity access JOIN FETCH access.user WHERE access.account = :account")
    List<UserAccountAccessEntity> findByAccount(@Param("account") AccountEntity account);
    boolean existsByUserAndAccount(UserEntity user, AccountEntity account);
    boolean existsByUserIdAndAccountId(UUID userId, UUID accountId);

    @Query(
            value = "SELECT a.* FROM accounts a " +
                    "JOIN user_account_access uaa ON a.id = uaa.account_id " +
                    "WHERE uaa.user_id = :userId " +
                    "AND a.deleted_at IS NULL " +
                    "ORDER BY a.created_at ASC",
            nativeQuery = true
    )
    List<AccountEntity> findActiveAccountsByUserId(UUID userId);

    @Query("SELECT u.account.id FROM UserAccountAccessEntity u WHERE u.user.id = :userId")
    List<UUID> findAccountIdsByUserId(@Param("userId") UUID userId);


}
