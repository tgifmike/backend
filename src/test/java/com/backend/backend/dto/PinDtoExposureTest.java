package com.backend.backend.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PinDtoExposureTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void accountUserDtoContainsStatusButNoCredentialSecrets() throws Exception {
        AccountUserDto user = new AccountUserDto(
                UUID.randomUUID(), "Employee", "employee@example.com", null,
                true, false, true, "USER", "MEMBER", Instant.now(), Instant.now(),
                true, false, null, 3
        );

        String json = objectMapper.writeValueAsString(user);

        assertThat(json).contains("pinConfigured", "credentialVersion");
        assertThat(json).doesNotContain(
                "pinLookupDigest", "onlinePinHash", "offlineVerifier",
                "encryptedOfflineVerifier", "failedAttempts", "encryptionKeyVersion"
        );
    }

    @Test
    void globalUserDtoHasNoAccountPinStatus() throws Exception {
        UserDto user = UserDto.builder().id(UUID.randomUUID()).userName("Employee").build();
        assertThat(objectMapper.writeValueAsString(user))
                .doesNotContain("pinConfigured", "pinLocked", "credentialVersion");
    }

    @Test
    void offlineBundleExposesOnlyTheOfflineVerifier() throws Exception {
        OfflinePinVerifierBundleDto bundle = new OfflinePinVerifierBundleDto(
                UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now().plusSeconds(3600), 12,
                List.of(new OfflinePinVerifierUserDto(
                        UUID.randomUUID(), "Employee", null, 6, "$argon2id$offline", 3
                ))
        );
        String json = objectMapper.writeValueAsString(bundle);
        assertThat(json).contains("offlineVerifier");
        assertThat(json).doesNotContain("onlinePinHash", "pinLookupDigest", "encryptedOfflineVerifier");
    }

    @Test
    void manualPinResponseDoesNotContainANullPinField() throws Exception {
        String json = objectMapper.writeValueAsString(PinManagementResponse.configured(2));
        assertThat(json).isEqualTo("{\"pinConfigured\":true,\"credentialVersion\":2}");
    }
}
