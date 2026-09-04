package com.backend.backend.dto;

import java.time.Instant;

public record PinStatusDto(
        boolean pinConfigured,
        boolean pinLocked,
        Instant pinLockedUntil,
        long credentialVersion
) {
}
