package com.backend.backend.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LocationDto {

    private UUID id;
    private String locationName;
    private String locationStreet;
    private String locationTown;
    private String locationState;
    private String locationZipCode;
    private String locationTimeZone;
    private boolean locationActive;
}
