package com.backend.backend.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;

public record ManualPinRequest(
        @NotBlank
        @Pattern(regexp = "^(?:\\d{4}|\\d{6})$", message = "PIN must contain exactly four or six digits")
        String pin
) {
    @Override
    public String toString() {
        return "ManualPinRequest[pin=<redacted>]";
    }
}
