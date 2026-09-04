package com.backend.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PinManagementResponse(
        boolean pinConfigured,
        long credentialVersion,
        String pin
) {
    public static PinManagementResponse configured(long credentialVersion) {
        return new PinManagementResponse(true, credentialVersion, null);
    }

    public static PinManagementResponse generated(long credentialVersion, String pin) {
        return new PinManagementResponse(true, credentialVersion, pin);
    }

    @Override
    public String toString() {
        return "PinManagementResponse[pinConfigured=" + pinConfigured
                + ", credentialVersion=" + credentialVersion
                + ", pin=" + (pin == null ? "null" : "<redacted>") + "]";
    }
}
