package com.backend.backend.dto;

import com.backend.backend.entity.UserEntity;

import java.util.UUID;

public record PinEmployeeResponse(
        UUID id, String userName, String userEmail, String appRole,
        String accessRole, boolean userActive, boolean invited, boolean pinConfigured
) {
    public static PinEmployeeResponse from(UserEntity user, boolean pinConfigured) {
        return new PinEmployeeResponse(user.getId(), user.getUserName(), user.getUserEmail(),
                user.getAppRole().name(), user.getAccessRole().name(), user.isUserActive(),
                user.isInvited(), pinConfigured);
    }
}
