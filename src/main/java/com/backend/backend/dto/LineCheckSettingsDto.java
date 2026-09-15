package com.backend.backend.dto;

import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LineCheckSettingsDto {
    private String dayOfWeek;
    private Integer dailyGoal;
    private LocalTime endOfDay;
}
