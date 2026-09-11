package com.backend.backend.controller;

import com.backend.backend.config.UserContext;
import com.backend.backend.dto.EulaAcceptanceRequest;
import com.backend.backend.dto.EulaAcceptanceStatusDto;
import com.backend.backend.service.UserEulaAcceptanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/me/eula-acceptance")
@RequiredArgsConstructor
public class UserEulaAcceptanceController {
    private final UserEulaAcceptanceService eulaService;

    @GetMapping
    public ResponseEntity<EulaAcceptanceStatusDto> getStatus() {
        return ResponseEntity.ok(eulaService.getStatus(currentUserId()));
    }

    @PostMapping
    public ResponseEntity<EulaAcceptanceStatusDto> accept(@Valid @RequestBody EulaAcceptanceRequest request) {
        return ResponseEntity.ok(eulaService.accept(currentUserId(), request));
    }

    private static java.util.UUID currentUserId() {
        java.util.UUID id = UserContext.getCurrentUser();
        if (id == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return id;
    }
}
