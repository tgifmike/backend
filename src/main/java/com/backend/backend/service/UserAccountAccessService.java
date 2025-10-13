package com.backend.backend.service;

import com.backend.backend.entity.AccountEntity;
import com.backend.backend.entity.UserAccountAccessEntity;
import com.backend.backend.entity.UserEntity;

import java.util.List;
import java.util.UUID;

public interface UserAccountAccessService {

    List<UserAccountAccessEntity> getAccountsForUser(UserEntity user);
    List<UserAccountAccessEntity> getUsersForAccount(AccountEntity account);
    UserAccountAccessEntity grantAccess(UserEntity user, AccountEntity account);
    void revokeAccess(UserEntity user, AccountEntity account);
    boolean userHasAccessToAccount(UUID userId, UUID accountId);

}
