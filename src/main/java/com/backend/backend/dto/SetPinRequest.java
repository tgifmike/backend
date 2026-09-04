package com.backend.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SetPinRequest(
        @NotBlank
        @Pattern(regexp = "^(?:\\d{4}|\\d{6})$", message = "PIN must contain exactly four or six digits")
        String pin
) {
    @Override
    public String toString() {
        return "SetPinRequest[pin=<redacted>]";
    }
}
