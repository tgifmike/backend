package com.backend.backend.repositories;

import com.backend.backend.entity.AccountHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccountHistoryRepository extends JpaRepository<AccountHistoryEntity, UUID> {

    List<AccountHistoryEntity> findByAccountIdOrderByChangeAtDesc(UUID accountId);

    //for history
    List<AccountHistoryEntity> findByAccountId(UUID accountId);



}

