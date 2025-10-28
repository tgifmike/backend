package com.backend.backend.repositories;



import com.backend.backend.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
    Optional<AccountEntity> findByAccountName(String accountName);
    boolean existsByAccountName(String accountName);
}