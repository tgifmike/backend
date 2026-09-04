package com.backend.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record AccountUserDto(
        UUID id,
        String userName,
        String userEmail,
        String userImage,
        boolean userActive,
        boolean firstLogin,
        boolean invited,
        String accessRole,
        String appRole,
        Instant createdAt,
        Instant updatedAt,
        boolean pinConfigured,
        boolean pinLocked,
        Instant pinLockedUntil,
        long credentialVersion
) {
}
