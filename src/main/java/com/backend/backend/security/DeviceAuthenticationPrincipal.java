package com.backend.backend.security;

import java.util.UUID;

public record DeviceAuthenticationPrincipal(UUID deviceId, UUID accountId, UUID locationId) {
}
