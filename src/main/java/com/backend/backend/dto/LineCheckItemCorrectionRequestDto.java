package com.backend.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineCheckItemCorrectionRequestDto {
    private Boolean corrected;
    private String correctiveNotes;
}
