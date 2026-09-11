package com.backend.backend.controller;

import com.backend.backend.dto.CreatePinEmployeeRequest;
import com.backend.backend.dto.PinEmployeeResponse;
import com.backend.backend.service.AccountAuthorizationService;
import com.backend.backend.service.PinEmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class PinEmployeeController {
    private final PinEmployeeService pinEmployeeService;
    private final AccountAuthorizationService authorizationService;

    @PostMapping("/{accountId}/pin-employees")
    public ResponseEntity<PinEmployeeResponse> create(
            @PathVariable UUID accountId,
            @Valid @RequestBody CreatePinEmployeeRequest request) {
        return ResponseEntity.ok(pinEmployeeService.create(
                accountId, request, authorizationService.currentActorId()));
    }
}
