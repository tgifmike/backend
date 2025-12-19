package com.backend.backend.controller;

import com.backend.backend.dto.AccountDto;
import com.backend.backend.entity.AccountEntity;
import com.backend.backend.entity.AccountHistoryEntity;
import com.backend.backend.entity.UserEntity;
import com.backend.backend.repositories.AccountHistoryRepository;
import com.backend.backend.service.AccountService;
import com.backend.backend.service.UserAccountAccessService;
import com.backend.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;




@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;
    private final UserAccountAccessService userAccountAccessService;
    private final UserService userService;
    private final AccountHistoryRepository accountHistoryRepository;

    public AccountController(AccountService accountService,
                             UserService userService,
                             UserAccountAccessService userAccountAccessService,
                             AccountHistoryRepository accountHistoryRepository
    ) {
        this.accountService = accountService;
        this.userAccountAccessService = userAccountAccessService;
        this.userService = userService;
        this.accountHistoryRepository = accountHistoryRepository;
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
    public ResponseEntity<AccountEntity> createAccount(
            @RequestParam UUID userId,
            @RequestBody AccountEntity account
    ) {
        UserEntity user = userService.getUserById(userId);
        AccountEntity created = accountService.createAccount(account, user);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }


    //giving access when user creates account
    @PostMapping("/createAccountWithAccess/{userId}")
    public ResponseEntity<AccountEntity> createAccountWithAccess(
            @PathVariable UUID userId,
            @RequestBody AccountEntity account
    ) {
        UserEntity user = userService.getUserById(userId);

        AccountEntity createdAccount =
                accountService.createAccount(account, user);

        userAccountAccessService.grantAccess(user, createdAccount);

        return new ResponseEntity<>(createdAccount, HttpStatus.CREATED);
    }




//    @PatchMapping("/{id}")
//    public ResponseEntity<AccountEntity> updateAccount(@PathVariable UUID id, @RequestBody Map<String, Object> updates) {
//        AccountEntity updated = accountService.partialUpdate(id, updates);
//        return ResponseEntity.ok(updated);
//    }


//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> deleteAccount(@PathVariable UUID id) {
//        accountService.deleteAccount(id);
//        return ResponseEntity.noContent().build();
//    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(
            @PathVariable UUID id,
            @RequestParam UUID userId
    ) {
        UserEntity user = userService.getUserById(userId);
        accountService.deleteAccount(id, user);
        return ResponseEntity.noContent().build();
    }



    //update user status
    @PatchMapping("/{id}")
    public ResponseEntity<AccountEntity> updateAccount(
            @PathVariable UUID id,
            @RequestParam UUID userId,
            @RequestBody AccountEntity incoming
    ) {
        UserEntity user = userService.getUserById(userId);
        AccountEntity updated = accountService.updateAccount(id, incoming, user);
        return ResponseEntity.ok(updated);
    }



    //checking is there is access
    @GetMapping("/by-id/{accountId}")
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

    @PutMapping("/{id}/image")
    public ResponseEntity<Void> uploadImage(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request){

        String base64Image = request.get("imageBase64");
        if(base64Image == null || base64Image.isEmpty()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No image");
        }

        accountService.updateAccountImage(id, base64Image);
        return ResponseEntity.ok().build();
    }

    //toggle active
    @PatchMapping("/{id}/active")
    public AccountDto toggleActive(
            @PathVariable UUID id,
            @RequestParam boolean active,
            @RequestParam UUID userId,
            @RequestParam String userName
    ) {
        return accountService.toggleAccountActive(id, active, userId, userName);
    }

    @GetMapping("/history")
    public List<AccountHistoryEntity> getAccountHistory(
            @RequestParam(required = false) UUID accountId) {

        if (accountId != null) {
            // Return history for a specific account
            return accountHistoryRepository.findByAccountId(accountId);
        } else {
            // Return all history (global)
            return accountHistoryRepository.findAll();
        }
    }






}
