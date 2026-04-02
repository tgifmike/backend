package com.backend.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SalesEmailDto {
    @NotBlank private String name;
    @Email private String email;
    @NotBlank private String restaurant;
    @Min(1) private Integer locations;
    @NotBlank private String message;
}
