package com.backend.backend.controller;

import com.backend.backend.dto.IpadEnrollmentRequest;
import com.backend.backend.dto.IpadEnrollmentResponse;
import com.backend.backend.dto.IpadDeviceSummaryDto;
import com.backend.backend.dto.OfflinePinEventBatchRequest;
import com.backend.backend.dto.OfflinePinEventBatchResponse;
import com.backend.backend.dto.OfflinePinVerifierBundleDto;
import com.backend.backend.service.AccountAuthorizationService;
import com.backend.backend.service.IpadDeviceService;
import com.backend.backend.service.OfflinePinEventService;
import com.backend.backend.service.UserAccountPinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/ipad/devices")
@RequiredArgsConstructor
public class IpadDeviceController {
    private final IpadDeviceService deviceService;
    private final UserAccountPinService pinService;
    private final OfflinePinEventService offlinePinEventService;
    private final AccountAuthorizationService authorizationService;

    @PostMapping("/enroll")
    public ResponseEntity<IpadEnrollmentResponse> enroll(@Valid @RequestBody IpadEnrollmentRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(deviceService.enroll(request, authorizationService.currentActorId()));
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<List<IpadDeviceSummaryDto>> listForAccount(@PathVariable UUID accountId) {
        return ResponseEntity.ok(deviceService.listForAccount(accountId, authorizationService.currentActorId()));
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> revoke(@PathVariable UUID deviceId) {
        deviceService.revoke(deviceId, authorizationService.currentActorId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{deviceId}/pin-verifiers")
    public ResponseEntity<OfflinePinVerifierBundleDto> pinVerifiers(@PathVariable UUID deviceId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(pinService.buildOfflineVerifierBundle(deviceId));
    }

    @PostMapping("/{deviceId}/pin-events/batch")
    public OfflinePinEventBatchResponse uploadEvents(
            @PathVariable UUID deviceId,
            @Valid @RequestBody OfflinePinEventBatchRequest request
    ) {
        return offlinePinEventService.acceptBatch(deviceId, request.events());
    }
}
