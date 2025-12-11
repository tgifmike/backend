package com.backend.backend.dto;

import com.backend.backend.config.OptionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OptionCreateDto {
    @NotNull
    private String optionName;

    private Boolean optionActive = true; // default to true
    private OptionType optionType = OptionType.TOOL;

    @NotNull
    private UUID accountId;
}
