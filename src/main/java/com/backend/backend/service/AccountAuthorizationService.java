package com.backend.backend.service;

import com.backend.backend.config.UserContext;
import com.backend.backend.entity.UserEntity;
import com.backend.backend.enums.AccessRole;
import com.backend.backend.enums.AppRole;
import com.backend.backend.repositories.UserAccountAccessRepository;
import com.backend.backend.repositories.UserRepository;
import com.backend.backend.exception.PinApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountAuthorizationService {
    private final UserRepository userRepository;
    private final UserAccountAccessRepository userAccountAccessRepository;

    public UUID currentActorId() {
        UUID actorId = UserContext.getCurrentUser();
        if (actorId == null) {
            throw new PinApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required");
        }
        return actorId;
    }

    public UserEntity requireGlobalAdmin(UUID actorId) {
        UserEntity actor = requireActiveActor(actorId);
        if (actor.getAccessRole() != AccessRole.ADMIN && actor.getAccessRole() != AccessRole.SRADMIN) {
            throw new PinApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Global administrator permission is required");
        }
        return actor;
    }

    public UserEntity requireCanManageAccount(UUID actorId, UUID accountId) {
        UserEntity actor = requireActiveActor(actorId);
        if (actor.getAccessRole() == AccessRole.ADMIN || actor.getAccessRole() == AccessRole.SRADMIN) {
            return actor;
        }
        boolean accountManager = actor.getAppRole() == AppRole.MANAGER
                && userAccountAccessRepository.existsByUserIdAndAccountId(actorId, accountId);
        if (!accountManager) {
            throw new PinApiException(
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_MANAGER_REQUIRED",
                    "The manager must belong to the requested account"
            );
        }
        return actor;
    }

    public UserEntity requireCanManagePins(UUID actorId, UUID accountId) {
        return requireCanManageAccount(actorId, accountId);
    }

    private UserEntity requireActiveActor(UUID actorId) {
        UserEntity actor = userRepository.findByIdAndDeletedAtIsNull(actorId)
                .orElseThrow(() -> new PinApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authenticated user not found"));
        if (!actor.isUserActive()) {
            throw new PinApiException(HttpStatus.FORBIDDEN, "ACTOR_INACTIVE", "Authenticated user is inactive");
        }
        return actor;
    }
}
