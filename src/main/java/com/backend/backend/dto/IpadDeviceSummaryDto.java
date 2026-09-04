package com.backend.backend.dto;

import com.backend.backend.entity.IpadDeviceEntity;

import java.time.Instant;
import java.util.UUID;

public record IpadDeviceSummaryDto(
        UUID id,
        UUID accountId,
        UUID locationId,
        String locationName,
        String deviceName,
        boolean active,
        Instant enrolledAt,
        Instant lastSeenAt,
        Instant revokedAt
) {
    public static IpadDeviceSummaryDto fromEntity(IpadDeviceEntity device) {
        return new IpadDeviceSummaryDto(
                device.getId(),
                device.getAccount().getId(),
                device.getLocation() == null ? null : device.getLocation().getId(),
                device.getLocation() == null ? null : device.getLocation().getLocationName(),
                device.getDeviceName(),
                device.isActive(),
                device.getEnrolledAt(),
                device.getLastSeenAt(),
                device.getRevokedAt()
        );
    }
}
