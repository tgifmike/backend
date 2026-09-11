package com.backend.backend.dto;

import java.time.Instant;

public record EulaAcceptanceStatusDto(
        boolean accepted,
        Integer acceptedVersion,
        Instant acceptedAt
) {}
