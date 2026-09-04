package com.backend.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PinCryptoServiceTest {
    private static final String LOOKUP_SECRET = "lookup-secret-with-at-least-32-characters";
    private static final String HASH_PEPPER = "hash-pepper-with-at-least-32-characters";
    private static final String ENCRYPTION_KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void lookupDigestIsStableAndAccountScoped() {
        PinCryptoService service = service(new SecureRandom());
        UUID firstAccount = UUID.randomUUID();
        UUID secondAccount = UUID.randomUUID();

        assertThat(service.lookupDigest(firstAccount, "048291"))
                .isEqualTo(service.lookupDigest(firstAccount, "048291"))
                .isNotEqualTo(service.lookupDigest(secondAccount, "048291"));
    }

    @Test
    void sameNumericPinCanBeAssignedInSeparateAccounts() {
        PinCryptoService service = service(new SecureRandom());
        assertThat(service.lookupDigest(UUID.randomUUID(), "1234"))
                .isNotEqualTo(service.lookupDigest(UUID.randomUUID(), "1234"));
    }

    @Test
    void onlineAndEncryptedOfflineVerifiersUseIndependentArgon2Inputs() {
        PinCryptoService service = service(new SecureRandom());
        UUID accountId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        String onlineHash = service.createOnlineHash(accountId, userId, "048291");
        PinCryptoService.EncryptedVerifier encrypted = service.createEncryptedOfflineVerifier("048291");
        String offlineHash = service.decryptOfflineVerifier(
                encrypted.ciphertext(), encrypted.nonce(), encrypted.keyVersion()
        );

        assertThat(onlineHash).startsWith("$argon2id$");
        assertThat(service.matchesOnlineHash(accountId, userId, "048291", onlineHash)).isTrue();
        assertThat(service.matchesOnlineHash(accountId, userId, "048292", onlineHash)).isFalse();
        assertThat(offlineHash)
                .startsWith("$argon2id$v=19$m=19456,t=2,p=1$")
                .isNotEqualTo(onlineHash);
        assertThat(Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8().matches("048291", offlineHash)).isTrue();
        assertThat(encrypted.ciphertext()).doesNotContain("048291").doesNotContain("argon2");
    }

    @Test
    void validatesOnlyFourOrSixNumericDigits() {
        PinCryptoService service = service(new SecureRandom());
        service.validatePin("0123");
        service.validatePin("012345");

        assertThatThrownBy(() -> service.validatePin("12345")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.validatePin("12a4")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.validateLength(5)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generatedPinPreservesLeadingZeroes() {
        PinCryptoService service = service(new SequenceSecureRandom(0, 4, 8, 2, 9, 1));
        assertThat(service.generatePin(6)).isEqualTo("048291");
    }

    private static PinCryptoService service(SecureRandom random) {
        return new PinCryptoService(LOOKUP_SECRET, HASH_PEPPER, ENCRYPTION_KEY, 1, random);
    }

    private static final class SequenceSecureRandom extends SecureRandom {
        private final int[] values;
        private int index;

        private SequenceSecureRandom(int... values) {
            this.values = values;
        }

        @Override
        public int nextInt(int bound) {
            return values[index++];
        }
    }
}
