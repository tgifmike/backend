package com.backend.backend.service;

import com.backend.backend.entity.AccountEntity;
import com.backend.backend.entity.IpadDeviceEntity;
import com.backend.backend.entity.LocationEntity;
import com.backend.backend.exception.PinApiException;
import com.backend.backend.repositories.AccountRepository;
import com.backend.backend.repositories.IpadDeviceRepository;
import com.backend.backend.repositories.LocationRepository;
import com.backend.backend.repositories.UserAccountAccessRepository;
import com.backend.backend.repositories.UserAccountPinRepository;
import com.backend.backend.repositories.UserLocationAccessRepository;
import com.backend.backend.repositories.UserRepository;
import com.backend.backend.security.DeviceAuthenticationPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IpadBundleAccessTest {
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void revokedDeviceCannotDownloadVerifierBundle() {
        UUID deviceId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        AccountEntity account = new AccountEntity();
        account.setId(accountId);
        LocationEntity location = new LocationEntity();
        location.setId(locationId);
        location.setAccount(account);
        IpadDeviceEntity device = new IpadDeviceEntity();
        device.setId(deviceId);
        device.setAccount(account);
        device.setLocation(location);
        device.setActive(false);
        IpadDeviceRepository devices = mock(IpadDeviceRepository.class);
        when(devices.findByIdForUpdate(deviceId)).thenReturn(Optional.of(device));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new DeviceAuthenticationPrincipal(deviceId, accountId, locationId), null,
                List.of(new SimpleGrantedAuthority("DEVICE_AUTH"))
        ));
        UserAccountPinService service = new UserAccountPinService(
                mock(UserAccountPinRepository.class), mock(AccountRepository.class), mock(UserRepository.class),
                mock(LocationRepository.class), mock(UserAccountAccessRepository.class),
                mock(UserLocationAccessRepository.class), devices, mock(AccountAuthorizationService.class),
                mock(PinCryptoService.class), new PinLockoutPolicy(), mock(PinRateLimitService.class),
                mock(PinActionTokenService.class), mock(PinAuditService.class),
                mock(AuditRequestMetadataProvider.class), Clock.systemUTC()
        );

        assertThatThrownBy(() -> service.buildOfflineVerifierBundle(deviceId))
                .isInstanceOf(PinApiException.class)
                .extracting("code")
                .isEqualTo("INVALID_DEVICE");
    }
}
