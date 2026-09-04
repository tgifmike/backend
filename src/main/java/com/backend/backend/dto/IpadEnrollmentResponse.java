package com.backend.backend.dto;

import java.util.UUID;

public record IpadEnrollmentResponse(UUID deviceId, String deviceToken) {
    @Override
    public String toString() {
        return "IpadEnrollmentResponse[deviceId=" + deviceId + ", deviceToken=<redacted>]";
    }
}
