package com.backend.backend.service;

import com.backend.backend.dto.CreatePinEmployeeRequest;
import com.backend.backend.dto.PinEmployeeResponse;
import com.backend.backend.entity.UserAccountAccessEntity;
import com.backend.backend.entity.UserEntity;
import com.backend.backend.entity.UserHistoryEntity;
import com.backend.backend.enums.AuthenticationMode;
import com.backend.backend.enums.HistoryType;
import com.backend.backend.exception.PinApiException;
import com.backend.backend.repositories.AccountRepository;
import com.backend.backend.repositories.UserAccountAccessRepository;
import com.backend.backend.repositories.UserAccountPinRepository;
import com.backend.backend.repositories.UserHistoryRepository;
import com.backend.backend.repositories.UserRepository;
import com.backend.backend.repositories.LocationRepository;
import com.backend.backend.repositories.UserLocationAccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PinEmployeeService {
    private final AccountAuthorizationService authorizationService;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final UserAccountAccessRepository accessRepository;
    private final UserAccountPinRepository pinRepository;
    private final UserHistoryRepository historyRepository;
    private final LocationRepository locationRepository;
    private final UserLocationAccessRepository locationAccessRepository;

    @Transactional
    public PinEmployeeResponse create(UUID accountId, CreatePinEmployeeRequest request, UUID actorId) {
        UserEntity actor = authorizationService.requireCanManageAccount(actorId, accountId);
        var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new PinApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found"));
        if (!Boolean.TRUE.equals(account.getAccountActive()) || account.getDeletedAt() != null) {
            throw new PinApiException(HttpStatus.CONFLICT, "ACCOUNT_INACTIVE", "Account is inactive");
        }
        var locationIds = request.locationIds().stream().distinct().toList();
        if (locationIds.isEmpty()) {
            throw new PinApiException(HttpStatus.BAD_REQUEST, "LOCATION_REQUIRED", "At least one location is required");
        }
        var locations = locationIds.stream()
                .map(id -> locationRepository.findById(id).orElseThrow(() ->
                        new PinApiException(HttpStatus.BAD_REQUEST, "LOCATION_INVALID", "Selected location is invalid")))
                .toList();
        if (locations.stream().anyMatch(location -> location.getAccount() == null
                || !accountId.equals(location.getAccount().getId())
                || !Boolean.TRUE.equals(location.getLocationActive())
                || location.getDeletedAt() != null)) {
            throw new PinApiException(HttpStatus.BAD_REQUEST, "LOCATION_INVALID", "All locations must belong to the account and be active");
        }
        UserEntity user = new UserEntity();
        user.setUserName(request.userName().trim());
        user.setUserActive(true);
        user.setInvited(false);
        user.setFirstLogin(false);
        user.setAuthenticationMode(AuthenticationMode.PIN_ONLY);
        user = userRepository.saveAndFlush(user);

        UserAccountAccessEntity access = new UserAccountAccessEntity();
        access.setUser(user);
        access.setAccount(account);
        accessRepository.save(access);
        for (var location : locations) {
            var locationAccess = new com.backend.backend.entity.UserLocationAccessEntity();
            locationAccess.setUser(user);
            locationAccess.setLocation(location);
            locationAccessRepository.save(locationAccess);
        }

        historyRepository.save(UserHistoryEntity.builder()
                .userId(user.getId()).userName(user.getUserName()).userEmail(null)
                .userActive(true).accessRole(user.getAccessRole().name()).appRole(user.getAppRole().name())
                .changeAt(Instant.now()).changedBy(actor.getId()).changedByName(actor.getUserName())
                .changeType(HistoryType.CREATED).build());
        return PinEmployeeResponse.from(user, false);
    }
}
