package com.backend.backend.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OfflinePinVerifierBundleDto(
        UUID accountId,
        UUID locationId,
        Instant generatedAt,
        Instant expiresAt,
        long bundleVersion,
        List<OfflinePinVerifierUserDto> users
) {
}
