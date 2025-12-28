package com.backend.backend.serviceImplementation;

import com.backend.backend.dto.AccountDto;
import com.backend.backend.entity.AccountEntity;
import com.backend.backend.entity.AccountHistoryEntity;
import com.backend.backend.entity.UserEntity;
import com.backend.backend.enums.HistoryType;
import com.backend.backend.repositories.AccountHistoryRepository;
import com.backend.backend.repositories.AccountRepository;
import com.backend.backend.service.AccountService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountHistoryRepository accountHistoryRepository;

    public AccountServiceImpl(AccountRepository accountRepository, AccountHistoryRepository accountHistoryRepository) {
        this.accountRepository = accountRepository;
        this.accountHistoryRepository = accountHistoryRepository;
    }

@Transactional
@Override
public AccountEntity createAccount(AccountEntity account, UserEntity user) {
    AccountEntity saved = accountRepository.save(account);

    accountHistoryRepository.save(
            AccountHistoryEntity.builder()
                    .accountId(saved.getId())
                    .accountName(saved.getAccountName())
                    .accountActive(saved.getAccountActive())
                    .changeType(HistoryType.CREATED)
                    .changeAt(Instant.now())
                    .changedBy(user.getId())
                    .changedByName(user.getUserName())
                    .build()
    );

    return saved;
}

@Transactional
@Override
public AccountEntity updateAccount(UUID id, AccountEntity incoming, UserEntity user) {
    AccountEntity existing = accountRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

    Map<String, String> oldValues = new HashMap<>();

    if (!Objects.equals(existing.getAccountName(), incoming.getAccountName())) {
        oldValues.put("accountName", existing.getAccountName());
        existing.setAccountName(incoming.getAccountName());
    }

    if (existing.getAccountActive() != incoming.getAccountActive()) {
        oldValues.put("accountActive", String.valueOf(existing.getAccountActive()));
        existing.setAccountActive(incoming.getAccountActive());
    }

    AccountEntity saved = accountRepository.save(existing);

    if (!oldValues.isEmpty()) {
        accountHistoryRepository.save(
                AccountHistoryEntity.builder()
                        .accountId(saved.getId())
                        .accountName(saved.getAccountName())
                        .accountActive(saved.getAccountActive())
                        .oldValues(oldValues)
                        .changeType(HistoryType.UPDATED)
                        .changeAt(Instant.now())
                        .changedBy(user.getId())
                        .changedByName(user.getUserName())
                        .build()
        );
    }

    return saved;
}

    @Override
    @Transactional
    public void deleteAccount(UUID id, UserEntity user) {
        AccountEntity account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        account.setDeletedAt(Instant.now());
        account.setDeletedBy(user.getId());

        accountRepository.save(account);

        accountHistoryRepository.save(
                AccountHistoryEntity.builder()
                        .accountId(account.getId())
                        .accountName(account.getAccountName())
                        .accountActive(account.getAccountActive())
                        .changeType(HistoryType.DELETED)
                        .changeAt(Instant.now())
                        .changedBy(user.getId())
                        .changedByName(user.getUserName())
                        .build()
        );
    }


    @Override
    public AccountEntity getAccountById(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    @Override
    public AccountEntity getAccountByName(String accountName) {
        return accountRepository.findByAccountName(accountName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    @Override
    public List<AccountEntity> getAllAccounts() {
        return accountRepository.findAll();
    }

    //toggle active
    @Override
    @Transactional
    public AccountDto toggleAccountActive(UUID accountId, boolean active, UUID userId, String userName) {
        AccountEntity account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        Map<String, String> oldValues = Map.of("accountActive", String.valueOf(account.getAccountActive()));

        account.setAccountActive(active);
        accountRepository.save(account);

        AccountHistoryEntity history = AccountHistoryEntity.builder()
                .accountId(account.getId())
                .accountName(account.getAccountName())
                .accountActive(account.getAccountActive())
                .changedBy(userId)
                .changedByName(userName)
                .changeAt(Instant.now())
                .changeType(HistoryType.UPDATED)
                .oldValues(oldValues)
                .build();

        accountHistoryRepository.save(history);

        return new AccountDto(account); // or map fields manually
    }





    @Override
    @Transactional
    public void updateAccountImage(UUID id, String base64Image) {
        AccountEntity account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        account.setImageBase64(base64Image);
        account.setUpdatedAt(Instant.now());
        accountRepository.save(account);
    }

    public AccountEntity partialUpdate(UUID id, Map<String, Object> updates) {
        AccountEntity existing = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (updates.containsKey("accountName")) {
            existing.setAccountName((String) updates.get("accountName"));
        }
        if (updates.containsKey("accountActive")) {
            existing.setAccountActive((Boolean) updates.get("accountActive"));
        }
        // Do NOT touch imageBase64 unless explicitly sent

        return accountRepository.save(existing);
    }

}

