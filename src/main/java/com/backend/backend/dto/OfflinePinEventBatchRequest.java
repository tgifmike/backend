package com.backend.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OfflinePinEventBatchRequest(@NotEmpty List<@Valid OfflinePinEventDto> events) {
}
