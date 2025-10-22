package com.backend.backend.serviceImplementation;

import com.backend.backend.dto.AccountDto;
import com.backend.backend.entity.AccountEntity;
import com.backend.backend.repositories.AccountRepository;
import com.backend.backend.service.AccountService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    public AccountServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public AccountEntity createAccount(AccountEntity account) {
        if (accountRepository.existsByAccountName(account.getAccountName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account name already exists");
        }
        return accountRepository.save(account);
    }


    @Override
    public AccountEntity updateAccount(UUID id, AccountEntity account) {
        AccountEntity existing = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        if (!existing.getAccountName().equals(account.getAccountName())
                && accountRepository.existsByAccountName(account.getAccountName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account name already exists");
        }

        existing.setAccountName(account.getAccountName());
        existing.setAccountActive(account.isAccountActive());
        existing.setImageBase64(account.getImageBase64());

        return accountRepository.save(existing);
    }

    @Override
    public void deleteAccount(UUID id) {
        if (!accountRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
        }
        accountRepository.deleteById(id);
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
    public AccountDto toggleActive(UUID id, boolean active) {
        AccountEntity user = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found: " + id));

        user.setAccountActive(active);
        user.setUpdatedAt(LocalDateTime.now());

        AccountEntity saved = accountRepository.save(user);

        // Manually map Entity → DTO
        return new AccountDto(
                saved.getId(),
                saved.getAccountName(),
                saved.getImageBase64(),
                saved.isAccountActive()
        );
    }

    @Override
    @Transactional
    public void updateAccountImage(UUID id, String base64Image) {
        AccountEntity account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        account.setImageBase64(base64Image);
        account.setUpdatedAt(LocalDateTime.now());
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

