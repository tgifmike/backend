package com.backend.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AccountPinVerificationRequest(
        @NotNull UUID accountId,
        @NotNull UUID deviceId,
        @NotBlank String pin
) {
    @Override
    public String toString() {
        return "AccountPinVerificationRequest[accountId=" + accountId
                + ", deviceId=" + deviceId + ", pin=<redacted>]";
    }
}
