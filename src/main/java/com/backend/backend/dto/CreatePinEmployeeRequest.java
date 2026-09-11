package com.backend.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record CreatePinEmployeeRequest(
        @NotBlank String userName,
        @NotEmpty List<UUID> locationIds
) {}
