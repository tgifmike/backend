package com.backend.backend.controller;

import com.backend.backend.dto.GeneratePinRequest;
import com.backend.backend.dto.PinManagementResponse;
import com.backend.backend.dto.PinUnlockResponse;
import com.backend.backend.dto.SetPinRequest;
import com.backend.backend.service.AccountAuthorizationService;
import com.backend.backend.service.UserAccountPinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/accounts/{accountId}/users/{userId}/pin")
@RequiredArgsConstructor
public class AccountUserPinController {
    private final UserAccountPinService pinService;
    private final AccountAuthorizationService authorizationService;

    @PutMapping
    public ResponseEntity<PinManagementResponse> setPin(
            @PathVariable UUID accountId,
            @PathVariable UUID userId,
            @Valid @RequestBody SetPinRequest request
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(pinService.setManualPin(
                        accountId, userId, request.pin(), authorizationService.currentActorId()
                ));
    }

    @PostMapping("/generate")
    public ResponseEntity<PinManagementResponse> generatePin(
            @PathVariable UUID accountId,
            @PathVariable UUID userId,
            @Valid @RequestBody GeneratePinRequest request
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(pinService.generatePin(
                        accountId, userId, request.length(), authorizationService.currentActorId()
                ));
    }

    @DeleteMapping
    public ResponseEntity<Void> revokePin(@PathVariable UUID accountId, @PathVariable UUID userId) {
        pinService.revokePin(accountId, userId, authorizationService.currentActorId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/unlock")
    public PinUnlockResponse unlockPin(@PathVariable UUID accountId, @PathVariable UUID userId) {
        return pinService.unlockPin(accountId, userId, authorizationService.currentActorId());
    }
}
