package com.backend.backend.service;

import com.backend.backend.entity.AccountEntity;

import java.util.List;
import java.util.UUID;

public interface AccountService {
    AccountEntity createAccount(AccountEntity account);
    AccountEntity updateAccount(UUID id, AccountEntity account);
    void deleteAccount(UUID id);
    AccountEntity getAccountById(UUID id);
    AccountEntity getAccountByName(String accountName);
    List<AccountEntity> getAllAccounts();
}
