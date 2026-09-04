package com.backend.backend.dto;

import com.backend.backend.enums.PinAuditEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;
import java.util.UUID;

public record OfflinePinEventDto(
        @NotNull UUID eventId,
        @PositiveOrZero long sequenceNumber,
        @NotNull PinAuditEventType eventType,
        @NotNull UUID accountId,
        @NotNull UUID locationId,
        @NotNull UUID userId,
        long credentialVersion,
        @NotNull Instant occurredAt,
        Instant lockoutUntil,
        UUID lineCheckId,
        @NotBlank String deviceSignature
) {
}
