package com.backend.backend.service;

import com.backend.backend.dto.EulaAcceptanceRequest;
import com.backend.backend.dto.EulaAcceptanceStatusDto;
import com.backend.backend.entity.UserEulaAcceptanceEntity;
import com.backend.backend.exception.PinApiException;
import com.backend.backend.repositories.UserEulaAcceptanceRepository;
import com.backend.backend.repositories.UserRepository;
import com.backend.backend.service.AuditRequestMetadataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserEulaAcceptanceService {
    private final UserEulaAcceptanceRepository acceptanceRepository;
    private final UserRepository userRepository;
    private final AuditRequestMetadataProvider metadataProvider;
    private final Clock clock;

    @Value("${eula.current-version:1}")
    private int currentVersion;

    @Transactional(readOnly = true)
    public EulaAcceptanceStatusDto getStatus(UUID userId) {
        var acceptance = acceptanceRepository.findTopByUserIdOrderByEulaVersionDesc(userId);
        return acceptance.map(value -> new EulaAcceptanceStatusDto(
                        value.getEulaVersion() >= currentVersion, value.getEulaVersion(), value.getAcceptedAt()))
                .orElseGet(() -> new EulaAcceptanceStatusDto(false, null, null));
    }

    @Transactional
    public EulaAcceptanceStatusDto accept(UUID userId, EulaAcceptanceRequest request) {
        if (request.version() != currentVersion) {
            throw new PinApiException(HttpStatus.CONFLICT, "EULA_VERSION_REQUIRED",
                    "The current EULA version must be accepted");
        }
        var user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new PinApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authenticated user not found"));
        if (!user.isUserActive()) {
            throw new PinApiException(HttpStatus.FORBIDDEN, "USER_INACTIVE", "User is inactive");
        }
        if (!acceptanceRepository.existsByUserIdAndEulaVersion(userId, request.version())) {
            var acceptance = new UserEulaAcceptanceEntity();
            acceptance.setUser(user);
            acceptance.setEulaVersion(request.version());
            acceptance.setAcceptedAt(clock.instant());
            acceptance.setIpAddress(metadataProvider.current().ipAddress());
            acceptanceRepository.save(acceptance);
        }
        return getStatus(userId);
    }
}
