package com.backend.backend.service;

import com.backend.backend.entity.*;

import javax.xml.stream.Location;
import java.util.List;
import java.util.UUID;

public interface UserLocationAccessService {

    List<UserLocationAccessEntity> getLocationsForUser(UserEntity user);
    List<UserLocationAccessEntity> getUsersForLocation(LocationEntity location);
    UserLocationAccessEntity grantAccess(UserEntity user, LocationEntity location);
    void revokeAccess(UserEntity user, LocationEntity location);
    boolean userHasAccessToLocation(UUID userId, UUID LocationId);
    List<LocationEntity> getLocationsForUser(UUID userId);
}
