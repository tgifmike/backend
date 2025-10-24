package com.backend.backend.serviceImplementation;

import com.backend.backend.entity.*;
import com.backend.backend.repositories.LocationRepository;
import com.backend.backend.repositories.UserAccountAccessRepository;
import com.backend.backend.repositories.UserLocationAccessRepository;
import com.backend.backend.service.UserLocationAccessService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserLocationAccessServiceImpl implements UserLocationAccessService {

    private final UserLocationAccessRepository userLocationAccessRepository;
    private final UserAccountAccessRepository userAccountAccessRepository;
    private final LocationRepository locationRepository;


    public UserLocationAccessServiceImpl(
            UserLocationAccessRepository userLocationAccessRepository,
            UserAccountAccessRepository userAccountAccessRepository,
            LocationRepository locationRepository
    ) {
        this.userLocationAccessRepository = userLocationAccessRepository;
        this.userAccountAccessRepository = userAccountAccessRepository;
        this.locationRepository = locationRepository;
    }

    @Override
    public List<UserLocationAccessEntity> getLocationsForUser(UserEntity user) {
        return userLocationAccessRepository.findByUser(user);
    }

    @Override
    public List<UserLocationAccessEntity> getUsersForLocation(LocationEntity location) {
        return userLocationAccessRepository.findByLocation(location);
    }

    @Override
    public UserLocationAccessEntity grantAccess(UserEntity user, LocationEntity location) {
        if (userLocationAccessRepository.existsByUserAndLocation(user, location)) {
            return null;
        }
        UserLocationAccessEntity access = new UserLocationAccessEntity();
        access.setUser(user);
        access.setLocation(location);
        return userLocationAccessRepository.save(access);
    }

    @Override
    public void revokeAccess(UserEntity user, LocationEntity location) {
        List<UserLocationAccessEntity> accesses = userLocationAccessRepository.findByUser(user);
        accesses.stream()
                .filter(a -> a.getLocation().getId().equals(location.getId()))
                .forEach(userLocationAccessRepository::delete);
    }

    @Override
    public boolean userHasAccessToLocation(UUID userId, UUID locationId) {
        return userLocationAccessRepository.existsByUserIdAndLocationId(userId, locationId);
    }

    @Override
    public List<LocationEntity> getLocationsForUser(UUID userId) {
        List<UUID> accountIds = userAccountAccessRepository.findAccountIdsByUserId(userId);

        if (accountIds.isEmpty()) {
            return List.of(); // No accounts = no locations
        }

        return locationRepository.findByAccount_IdIn(accountIds);
    }
}