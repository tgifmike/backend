package com.backend.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PinVerificationRequest(
        UUID accountId,
        UUID locationId,
        UUID userId,
        @NotNull UUID deviceId,
        @NotBlank String pin
) {
    @Override
    public String toString() {
        return "PinVerificationRequest[accountId=" + accountId
                + ", locationId=" + locationId
                + ", userId=" + userId
                + ", deviceId=" + deviceId
                + ", pin=<redacted>]";
    }
}
