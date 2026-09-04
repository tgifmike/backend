package com.backend.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record IpadEnrollmentRequest(
        @NotNull UUID accountId,
        UUID locationId,
        @NotBlank String deviceName,
        @NotBlank String devicePublicKey
) {
}
