package com.backend.backend.dto;

import com.backend.backend.entity.LocationEntity;
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
    private Double locationLongitude;
    private Double locationLatitude;
    private boolean geocodedFromZipFallback;
    private UUID accountId;

//    public LocationDto(UUID id, String locationName, boolean locationActive) {
//        this.id = id;
//        this.locationName = locationName;
//        this.locationActive = locationActive;
//    }

    public static LocationDto fromEntity(LocationEntity loc) {
        if (loc == null) return null;

        return LocationDto.builder()
                .id(loc.getId())
                .locationName(loc.getLocationName())
                .locationStreet(loc.getLocationStreet())
                .locationTown(loc.getLocationTown())
                .locationState(loc.getLocationState())
                .locationZipCode(loc.getLocationZipCode())
                .locationTimeZone(loc.getLocationTimeZone())
                .locationLatitude(loc.getLocationLatitude())
                .locationLongitude(loc.getLocationLongitude())
                .geocodedFromZipFallback(loc.getGeocodedFromZipFallback())
                .locationActive(loc.isLocationActive())
                .accountId(loc.getAccount() != null ? loc.getAccount().getId() : null)
                .build();
    }
}
