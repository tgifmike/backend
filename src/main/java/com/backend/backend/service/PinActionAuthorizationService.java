package com.backend.backend.service;

import com.backend.backend.entity.IpadDeviceEntity;
import com.backend.backend.entity.LineCheckEntity;
import com.backend.backend.entity.StationEntity;
import com.backend.backend.entity.UserAccountPinEntity;
import com.backend.backend.enums.OfflineVerificationStatus;
import com.backend.backend.enums.PinCredentialStatus;
import com.backend.backend.exception.PinApiException;
import com.backend.backend.repositories.IpadDeviceRepository;
import com.backend.backend.repositories.LineCheckRepository;
import com.backend.backend.repositories.LineCheckItemRepository;
import com.backend.backend.repositories.LocationRepository;
import com.backend.backend.repositories.StationRepository;
import com.backend.backend.repositories.UserAccountAccessRepository;
import com.backend.backend.repositories.UserAccountPinRepository;
import com.backend.backend.repositories.UserLocationAccessRepository;
import com.backend.backend.security.PinActionPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PinActionAuthorizationService {
    private final UserAccountPinRepository pinRepository;
    private final IpadDeviceRepository deviceRepository;
    private final UserAccountAccessRepository accountAccessRepository;
    private final UserLocationAccessRepository locationAccessRepository;
    private final StationRepository stationRepository;
    private final LocationRepository locationRepository;
    private final LineCheckRepository lineCheckRepository;
    private final LineCheckItemRepository lineCheckItemRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public void validateToken(PinActionPrincipal principal) {
        UserAccountPinEntity credential = pinRepository
                .findByAccountIdAndUserId(principal.accountId(), principal.userId())
                .orElseThrow(this::forbidden);
        if (credential.getStatus() != PinCredentialStatus.ACTIVE
                || credential.getCredentialVersion() != principal.credentialVersion()
                || !credential.getUser().isUserActive()
                || credential.getUser().getDeletedAt() != null) {
            throw forbidden();
        }
        IpadDeviceEntity device = deviceRepository.findByIdAndActiveTrueWithScope(principal.deviceId())
                .orElseThrow(this::forbidden);
        if (!device.getAccount().getId().equals(principal.accountId())
                || !Boolean.TRUE.equals(device.getAccount().getAccountActive())
                || device.getAccount().getDeletedAt() != null
                || !accountAccessRepository.existsByUserIdAndAccountId(principal.userId(), principal.accountId())
                || (principal.locationId() != null && device.getLocation() != null
                    && !device.getLocation().getId().equals(principal.locationId()))
                || (principal.locationId() != null
                    && (!locationAccessRepository.existsByUserIdAndLocationId(principal.userId(), principal.locationId())))
                || (device.getLocation() != null
                    && (!Boolean.TRUE.equals(device.getLocation().getLocationActive())
                        || device.getLocation().getDeletedAt() != null))) {
            throw forbidden();
        }
    }

    @Transactional(readOnly = true)
    public void validateCreate(PinActionPrincipal principal, UUID userId, List<UUID> stationIds) {
        if (!principal.userId().equals(userId) || stationIds == null || stationIds.isEmpty()) {
            throw forbidden();
        }
        IpadDeviceEntity device = deviceRepository.findByIdAndActiveTrueWithScope(principal.deviceId()).orElseThrow(this::forbidden);
        UUID selectedLocationId = principal.locationId();
        if (selectedLocationId == null && device.getLocation() != null) {
            selectedLocationId = device.getLocation().getId();
        }
        for (UUID stationId : stationIds) {
            StationEntity station = stationRepository.findById(stationId).orElseThrow(this::forbidden);
            if (station.getLocation() == null
                    || !station.getLocation().getAccount().getId().equals(principal.accountId())
                    || !Boolean.TRUE.equals(station.getLocation().getLocationActive())
                    || station.getLocation().getDeletedAt() != null
                    || (selectedLocationId != null && !station.getLocation().getId().equals(selectedLocationId))
                    || !locationAccessRepository.existsByUserIdAndLocationId(
                        principal.userId(), station.getLocation().getId())) {
                throw forbidden();
            }
            if (selectedLocationId == null) {
                selectedLocationId = station.getLocation().getId();
            }
        }
    }

    @Transactional(readOnly = true)
    public void validateLocationSelection(PinActionPrincipal principal, UUID locationId) {
        if (locationId == null
                || !locationRepository.findById(locationId)
                    .filter(location -> location.getAccount() != null
                            && location.getAccount().getId().equals(principal.accountId())
                            && Boolean.TRUE.equals(location.getLocationActive())
                            && location.getDeletedAt() == null)
                    .isPresent()
                || !locationAccessRepository.existsByUserIdAndLocationId(principal.userId(), locationId)) {
            throw forbidden();
        }
        if (principal.locationId() != null && !principal.locationId().equals(locationId)) {
            throw forbidden();
        }
        IpadDeviceEntity device = deviceRepository.findByIdAndActiveTrueWithScope(principal.deviceId())
                .orElseThrow(this::forbidden);
        if (device.getLocation() != null && !device.getLocation().getId().equals(locationId)) {
            throw forbidden();
        }
    }

    @Transactional(readOnly = true)
    public void validateSave(PinActionPrincipal principal, UUID lineCheckId, UUID userId) {
        LineCheckEntity lineCheck = lineCheckRepository.findById(lineCheckId).orElseThrow(this::forbidden);
        if (!principal.userId().equals(userId)
                || lineCheck.getUser() == null
                || !lineCheck.getUser().getId().equals(principal.userId())) {
            throw forbidden();
        }
        UUID lineCheckLocation = null;
        for (var lineCheckStation : lineCheck.getStations()) {
            if (lineCheckStation.getStation() == null || lineCheckStation.getStation().getLocation() == null) {
                throw forbidden();
            }
            UUID stationLocation = lineCheckStation.getStation().getLocation().getId();
            if (lineCheckLocation == null) {
                lineCheckLocation = stationLocation;
            } else if (!lineCheckLocation.equals(stationLocation)) {
                throw forbidden();
            }
        }
        if (lineCheckLocation == null
                || (principal.locationId() != null && !principal.locationId().equals(lineCheckLocation))
                || !locationAccessRepository.existsByUserIdAndLocationId(principal.userId(), lineCheckLocation)) {
            throw forbidden();
        }
        IpadDeviceEntity device = deviceRepository.findByIdAndActiveTrueWithScope(principal.deviceId()).orElseThrow(this::forbidden);
        if (device.getLocation() != null && !device.getLocation().getId().equals(lineCheckLocation)) {
            throw forbidden();
        }
    }

    @Transactional(readOnly = true)
    public void validateCorrection(PinActionPrincipal principal, UUID itemId) {
        var item = lineCheckItemRepository.findById(itemId).orElseThrow(this::forbidden);
        var station = item.getLineCheckStation() == null ? null : item.getLineCheckStation().getStation();
        var location = station == null ? null : station.getLocation();
        if (location == null
                || location.getAccount() == null
                || !location.getAccount().getId().equals(principal.accountId())
                || !Boolean.TRUE.equals(location.getLocationActive())
                || location.getDeletedAt() != null
                || !locationAccessRepository.existsByUserIdAndLocationId(principal.userId(), location.getId())) {
            throw forbidden();
        }
    }

    @Transactional
    public void recordOnlineAuthentication(UUID lineCheckId, PinActionPrincipal principal) {
        LineCheckEntity lineCheck = lineCheckRepository.findById(lineCheckId).orElseThrow(this::forbidden);
        lineCheck.setAuthDeviceId(principal.deviceId());
        lineCheck.setAuthAccountId(principal.accountId());
        lineCheck.setAuthLocationId(principal.locationId() != null
                ? principal.locationId()
                : lineCheck.getStations().stream()
                    .filter(station -> station.getStation() != null && station.getStation().getLocation() != null)
                    .map(station -> station.getStation().getLocation().getId())
                    .findFirst()
                    .orElse(null));
        lineCheck.setAuthUserId(principal.userId());
        lineCheck.setAuthCredentialVersion(principal.credentialVersion());
        lineCheck.setAuthVerifiedAt(clock.instant());
        lineCheck.setVerificationStatus(OfflineVerificationStatus.CURRENT);
        lineCheckRepository.save(lineCheck);
    }

    private PinApiException forbidden() {
        return new PinApiException(HttpStatus.FORBIDDEN, "PIN_TOKEN_SCOPE_FORBIDDEN", "PIN action token is no longer valid");
    }
}
