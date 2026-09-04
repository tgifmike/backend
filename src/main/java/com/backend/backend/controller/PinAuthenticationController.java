package com.backend.backend.controller;

import com.backend.backend.dto.PinVerificationRequest;
import com.backend.backend.dto.PinVerificationResponse;
import com.backend.backend.dto.AccountPinVerificationRequest;
import com.backend.backend.service.UserAccountPinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/auth/pin")
@RequiredArgsConstructor
public class PinAuthenticationController {
    private final UserAccountPinService pinService;

    @PostMapping("/verify")
    public ResponseEntity<PinVerificationResponse> verify(@Valid @RequestBody PinVerificationRequest request) {
        PinVerificationResponse response = request.userId() == null
                ? (request.accountId() == null
                    ? pinService.verifyOnlineByDevicePin(request.deviceId(), request.pin())
                    : pinService.verifyOnlineByAccountPin(request.accountId(), request.deviceId(), request.pin()))
                : pinService.verifyOnline(request.accountId(), request.locationId(), request.userId(),
                        request.pin(), request.deviceId());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    @PostMapping("/verify/account")
    public ResponseEntity<PinVerificationResponse> verifyAccountPin(
            @Valid @RequestBody AccountPinVerificationRequest request) {
        PinVerificationResponse response = pinService.verifyOnlineByAccountPin(
                request.accountId(), request.deviceId(), request.pin());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }
}
