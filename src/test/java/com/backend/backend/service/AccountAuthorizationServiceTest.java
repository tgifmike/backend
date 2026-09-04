package com.backend.backend.service;

import com.backend.backend.entity.UserEntity;
import com.backend.backend.enums.AccessRole;
import com.backend.backend.enums.AppRole;
import com.backend.backend.exception.PinApiException;
import com.backend.backend.repositories.UserAccountAccessRepository;
import com.backend.backend.repositories.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountAuthorizationServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final UserAccountAccessRepository access = mock(UserAccountAccessRepository.class);
    private final AccountAuthorizationService service = new AccountAuthorizationService(users, access);

    @Test
    void managerMustBelongToRequestedAccount() {
        UUID actorId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UserEntity manager = user(actorId, AccessRole.USER, AppRole.MANAGER);
        when(users.findByIdAndDeletedAtIsNull(actorId)).thenReturn(Optional.of(manager));
        when(access.existsByUserIdAndAccountId(actorId, accountId)).thenReturn(false);

        assertThatThrownBy(() -> service.requireCanManagePins(actorId, accountId))
                .isInstanceOf(PinApiException.class)
                .extracting("code")
                .isEqualTo("ACCOUNT_MANAGER_REQUIRED");

        when(access.existsByUserIdAndAccountId(actorId, accountId)).thenReturn(true);
        assertThatCode(() -> service.requireCanManagePins(actorId, accountId)).doesNotThrowAnyException();
    }

    @Test
    void ordinaryMemberCannotManagePinsButGlobalAdminCan() {
        UUID accountId = UUID.randomUUID();
        UserEntity member = user(UUID.randomUUID(), AccessRole.USER, AppRole.MEMBER);
        UserEntity admin = user(UUID.randomUUID(), AccessRole.ADMIN, AppRole.MEMBER);
        when(users.findByIdAndDeletedAtIsNull(member.getId())).thenReturn(Optional.of(member));
        when(users.findByIdAndDeletedAtIsNull(admin.getId())).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.requireCanManagePins(member.getId(), accountId))
                .isInstanceOf(PinApiException.class);
        assertThatCode(() -> service.requireCanManagePins(admin.getId(), accountId)).doesNotThrowAnyException();
    }

    private static UserEntity user(UUID id, AccessRole accessRole, AppRole appRole) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setAccessRole(accessRole);
        user.setAppRole(appRole);
        user.setUserActive(true);
        return user;
    }
}
