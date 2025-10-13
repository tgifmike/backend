package com.backend.backend.controller;

import com.backend.backend.dto.AccountDto;
import com.backend.backend.dto.UserDto;
import com.backend.backend.entity.AccountEntity;
import com.backend.backend.service.AccountService;
import com.backend.backend.service.UserAccountAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

//@CrossOrigin(origins = {
//        "http://localhost:3000"
//})

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;
    private final UserAccountAccessService userAccountAccessService;

    public AccountController(AccountService accountService, UserAccountAccessService userAccountAccessService) {
        this.accountService = accountService;
        this.userAccountAccessService = userAccountAccessService;
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

    @PatchMapping("/{id}")
    public ResponseEntity<AccountEntity> updateAccount(@PathVariable UUID id, @RequestBody AccountEntity account) {
        return ResponseEntity.ok(accountService.updateAccount(id, account));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable UUID id) {
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }

    //update user status
    @PatchMapping("/{id}/active")
    public ResponseEntity<AccountDto> toggleActive(
            @PathVariable UUID id,
            @RequestParam boolean active
    ) {
        AccountDto updated = accountService.toggleActive(id, active);
        return ResponseEntity.ok(updated);
    }

    //checking is there is access
    @GetMapping("/{accountId}")
    public ResponseEntity<?> getAccountById(
            @PathVariable UUID accountId,
            @RequestParam UUID userId // can come from session token or query param
    ) {
        boolean hasAccess = userAccountAccessService.userHasAccessToAccount(userId, accountId);

        if (!hasAccess) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You do not have access to this account.");
        }

        AccountEntity account = accountService.getAccountById(accountId);
        return ResponseEntity.ok(account);
    }

}
