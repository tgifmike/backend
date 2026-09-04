package com.backend.backend.dto;

public record PinVerificationResponse(
        boolean verified,
        String employeeActionToken,
        long expiresInSeconds,
        java.util.UUID userId,
        String userName,
        java.util.UUID accountId
) {
    @Override
    public String toString() {
        return "PinVerificationResponse[verified=" + verified
                + ", employeeActionToken=<redacted>"
                + ", expiresInSeconds=" + expiresInSeconds
                + ", userId=" + userId
                + ", userName=" + userName
                + ", accountId=" + accountId + "]";
    }
}
