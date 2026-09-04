package com.backend.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.AssertTrue;
import com.fasterxml.jackson.annotation.JsonIgnore;

public record GeneratePinRequest(@Min(4) @Max(6) int length) {
    @AssertTrue(message = "PIN length must be 4 or 6")
    @JsonIgnore
    public boolean isSupportedLength() {
        return length == 4 || length == 6;
    }
}
