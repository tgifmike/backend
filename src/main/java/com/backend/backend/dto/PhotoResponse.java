package com.backend.backend.dto;

import com.backend.backend.enums.PhotoType;

import java.time.Instant;
import java.util.UUID;

public record PhotoResponse(
        UUID id,
        String s3Key,
        String originalFileName,
        String contentType,
        PhotoType photoType,
        String notes,
        Instant createdAt,
        UUID createdBy,
        String url
) {
}
