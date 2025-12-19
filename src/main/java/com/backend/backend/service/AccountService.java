package com.backend.backend.service;

import com.backend.backend.dto.AccountDto;
import com.backend.backend.entity.AccountEntity;
import com.backend.backend.entity.UserEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AccountService {
    //AccountEntity createAccount(AccountEntity account);
    AccountEntity createAccount(AccountEntity account, UserEntity user);

    //AccountEntity updateAccount(UUID id, AccountEntity account);
    AccountEntity updateAccount(UUID id, AccountEntity incoming, UserEntity user);
    //void deleteAccount(UUID id);
    //void deleteAccount(UUID id, UUID userId);
    void deleteAccount(UUID id, UserEntity user);

    AccountEntity getAccountById(UUID id);
    AccountEntity getAccountByName(String accountName);
    List<AccountEntity> getAllAccounts();
    AccountDto toggleAccountActive(UUID accountId, boolean active, UUID userId, String userName);
    void updateAccountImage(UUID id, String base64Image);
    AccountEntity partialUpdate(UUID id, Map<String, Object> updates);
}
