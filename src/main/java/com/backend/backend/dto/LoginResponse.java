package com.backend.backend.dto;

import java.util.UUID;

public record LoginResponse(
        String token,
        UUID userId,
        String email,
        String name,
        String appRole,
        String accessRole,
        boolean hasAccountAccess,
        String userImage
) {}
