package com.backend.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EulaAcceptanceRequest(@NotNull @Positive Integer version) {}
