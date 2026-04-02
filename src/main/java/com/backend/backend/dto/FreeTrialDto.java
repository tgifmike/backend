package com.backend.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FreeTrialDto {
    private String name;
    private String email;
    private String restaurant;
    private Integer locations;
    private String message; // optional notes or details
}
