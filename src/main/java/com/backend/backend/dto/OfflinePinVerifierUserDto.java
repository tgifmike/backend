package com.backend.backend.dto;

import java.util.UUID;

public record OfflinePinVerifierUserDto(
        UUID userId,
        String userName,
        String userImage,
        int pinLength,
        String offlineVerifier,
        long credentialVersion
) {
    @Override
    public String toString() {
        return "OfflinePinVerifierUserDto[userId=" + userId
                + ", userName=" + userName
                + ", pinLength=" + pinLength
                + ", offlineVerifier=<redacted>"
                + ", credentialVersion=" + credentialVersion + "]";
    }
}
