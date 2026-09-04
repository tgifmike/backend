package com.backend.backend.security;

import java.util.UUID;

public record PinActionPrincipal(
        UUID userId,
        UUID accountId,
        UUID locationId,
        UUID deviceId,
        long credentialVersion
) {
}
