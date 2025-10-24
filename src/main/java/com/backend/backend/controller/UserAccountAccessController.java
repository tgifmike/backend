package com.backend.backend.controller;

import com.backend.backend.dto.AccountDto;
import com.backend.backend.dto.LocationDto;
import com.backend.backend.entity.AccountEntity;
import com.backend.backend.entity.UserAccountAccessEntity;
import com.backend.backend.entity.UserEntity;
import com.backend.backend.service.AccountService;
import com.backend.backend.service.UserAccountAccessService;
import com.backend.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

//@CrossOrigin(origins = {
//        "http://localhost:3000"
//})

@RestController
@RequestMapping("/user-access")

public class UserAccountAccessController {

    private final UserAccountAccessService userAccountAccessService;
    private final UserService userService;
    private final AccountService accountService;

    public UserAccountAccessController(UserAccountAccessService userAccountAccessService, UserService userService, AccountService accountService) {
        this.userAccountAccessService = userAccountAccessService;
        this.userService = userService;
        this.accountService = accountService;
    }

    @GetMapping("/{userId}/accounts")
    public ResponseEntity<List<AccountDto>> getAccountsForUser(@PathVariable UUID userId) {
        UserEntity user = userService.getUserById(userId);
        List<AccountDto> accounts = userAccountAccessService.getAccountsForUser(user)
                .stream()
                .map(access -> new AccountDto(
                        access.getAccount().getId(),
                        access.getAccount().getAccountName(),
                        access.getAccount().getImageBase64(),
                        access.getAccount().isAccountActive()
                ))
                .toList();
        return ResponseEntity.ok(accounts);
    }



    @PostMapping("/{userId}/accounts/{accountId}")
    public UserAccountAccessEntity grantAccess(@PathVariable UUID userId,
                                               @PathVariable UUID accountId) {
        UserEntity user = userService.getUserById(userId);
        AccountEntity account = accountService.getAccountById(accountId);
        return userAccountAccessService.grantAccess(user, account);
    }

    @DeleteMapping("/{userId}/accounts/{accountId}")
    public void revokeAccess(@PathVariable UUID userId,
                             @PathVariable UUID accountId) {
        UserEntity user = userService.getUserById(userId);
        AccountEntity account = accountService.getAccountById(accountId);
        userAccountAccessService.revokeAccess(user, account);
    }



}
