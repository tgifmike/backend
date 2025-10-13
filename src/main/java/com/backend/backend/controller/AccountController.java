package com.backend.backend.controller;

import com.backend.backend.entity.AccountEntity;
import com.backend.backend.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = {
        "http://localhost:3000"
})

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<AccountEntity>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @GetMapping("/{accountName}")
    public ResponseEntity<AccountEntity> getAccountByName(@PathVariable String accountName) {
        return ResponseEntity.ok(accountService.getAccountByName(accountName));
    }

    @PostMapping("/createAccount")
    public ResponseEntity<AccountEntity> createAccount(@RequestBody AccountEntity account) {
        return new ResponseEntity<>(accountService.createAccount(account), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountEntity> updateAccount(@PathVariable UUID id, @RequestBody AccountEntity account) {
        return ResponseEntity.ok(accountService.updateAccount(id, account));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable UUID id) {
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
}
