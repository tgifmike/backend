package com.backend.backend.repositories;

import com.backend.backend.entity.AccountEntity;
import com.backend.backend.entity.UserAccountAccessEntity;
import com.backend.backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserAccountAccessRepository extends JpaRepository<UserAccountAccessEntity, UUID> {
    List<UserAccountAccessEntity> findByUser(UserEntity user);
    List<UserAccountAccessEntity> findByAccount(AccountEntity account);
    boolean existsByUserAndAccount(UserEntity user, AccountEntity account);
}
