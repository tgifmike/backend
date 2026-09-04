package com.backend.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Service
public class PinCryptoService {
    private static final int ARGON2_MEMORY_KIB = 19 * 1024;
    private static final int ARGON2_ITERATIONS = 2;
    private static final int ARGON2_PARALLELISM = 1;
    private static final int GCM_NONCE_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private final byte[] lookupSecret;
    private final byte[] hashPepper;
    private final SecretKeySpec offlineEncryptionKey;
    private final int encryptionKeyVersion;
    private final SecureRandom secureRandom;
    private final Argon2PasswordEncoder onlineEncoder;
    private final Argon2PasswordEncoder offlineEncoder;

    @Autowired
    public PinCryptoService(
            @Value("${pin.lookup-secret}") String lookupSecret,
            @Value("${pin.hash-pepper}") String hashPepper,
            @Value("${pin.offline-encryption-key}") String offlineEncryptionKey,
            @Value("${pin.offline-encryption-key-version:1}") int encryptionKeyVersion
    ) {
        this(lookupSecret, hashPepper, offlineEncryptionKey, encryptionKeyVersion, new SecureRandom());
    }

    PinCryptoService(
            String lookupSecret,
            String hashPepper,
            String offlineEncryptionKey,
            int encryptionKeyVersion,
            SecureRandom secureRandom
    ) {
        this.lookupSecret = requireSecret("PIN_LOOKUP_SECRET", lookupSecret);
        this.hashPepper = requireSecret("PIN_HASH_PEPPER", hashPepper);
        byte[] encryptionKey = decodeEncryptionKey(offlineEncryptionKey);
        this.offlineEncryptionKey = new SecretKeySpec(encryptionKey, "AES");
        this.encryptionKeyVersion = encryptionKeyVersion;
        this.secureRandom = secureRandom;
        this.onlineEncoder = new Argon2PasswordEncoder(
                16,
                32,
                ARGON2_PARALLELISM,
                ARGON2_MEMORY_KIB,
                ARGON2_ITERATIONS
        );
        this.offlineEncoder = new Argon2PasswordEncoder(
                16,
                32,
                ARGON2_PARALLELISM,
                ARGON2_MEMORY_KIB,
                ARGON2_ITERATIONS
        );
    }

    public String lookupDigest(UUID accountId, String pin) {
        validatePin(pin);
        return base64Url(hmac(lookupSecret, accountId + ":" + pin));
    }

    public String createOnlineHash(UUID accountId, UUID userId, String pin) {
        return onlineEncoder.encode(pepperedInput(accountId, userId, pin));
    }

    public boolean matchesOnlineHash(UUID accountId, UUID userId, String pin, String encodedHash) {
        if (encodedHash == null) {
            return false;
        }
        try {
            return onlineEncoder.matches(pepperedInput(accountId, userId, pin), encodedHash);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public EncryptedVerifier createEncryptedOfflineVerifier(String pin) {
        validatePin(pin);
        String encodedVerifier = offlineEncoder.encode(pin);
        return encryptOfflineVerifier(encodedVerifier);
    }

    public String decryptOfflineVerifier(String ciphertext, String nonce, Integer keyVersion) {
        if (ciphertext == null || nonce == null || keyVersion == null) {
            throw new IllegalStateException("Offline verifier is unavailable");
        }
        if (keyVersion != encryptionKeyVersion) {
            throw new IllegalStateException("Unsupported PIN encryption key version");
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    offlineEncryptionKey,
                    new GCMParameterSpec(GCM_TAG_BITS, Base64.getUrlDecoder().decode(nonce))
            );
            byte[] plaintext = cipher.doFinal(Base64.getUrlDecoder().decode(ciphertext));
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException("Unable to decrypt offline PIN verifier", ex);
        }
    }

    public String generatePin(int length) {
        validateLength(length);
        StringBuilder pin = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            pin.append(secureRandom.nextInt(10));
        }
        return pin.toString();
    }

    public void validatePin(String pin) {
        if (pin == null || !(pin.length() == 4 || pin.length() == 6) || !pin.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("PIN must contain exactly four or six digits");
        }
    }

    public void validateLength(int length) {
        if (length != 4 && length != 6) {
            throw new IllegalArgumentException("PIN length must be 4 or 6");
        }
    }

    private String pepperedInput(UUID accountId, UUID userId, String pin) {
        validatePin(pin);
        return base64Url(hmac(hashPepper, accountId + ":" + userId + ":" + pin));
    }

    private EncryptedVerifier encryptOfflineVerifier(String verifier) {
        byte[] nonce = new byte[GCM_NONCE_BYTES];
        secureRandom.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, offlineEncryptionKey, new GCMParameterSpec(GCM_TAG_BITS, nonce));
            byte[] ciphertext = cipher.doFinal(verifier.getBytes(StandardCharsets.UTF_8));
            return new EncryptedVerifier(
                    base64Url(ciphertext),
                    base64Url(nonce),
                    encryptionKeyVersion
            );
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to encrypt offline PIN verifier", ex);
        }
    }

    private static byte[] hmac(byte[] secret, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", ex);
        }
    }

    private static String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static byte[] requireSecret(String name, String value) {
        if (value == null || value.length() < 32) {
            throw new IllegalStateException(name + " must contain at least 32 characters");
        }
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] decodeEncryptionKey(String value) {
        try {
            byte[] key = Base64.getDecoder().decode(value == null ? "" : value);
            if (key.length != 32) {
                throw new IllegalStateException("PIN_OFFLINE_ENCRYPTION_KEY must be a Base64-encoded 256-bit key");
            }
            return key;
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("PIN_OFFLINE_ENCRYPTION_KEY must be valid Base64", ex);
        }
    }

    public static boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.US_ASCII),
                right.getBytes(StandardCharsets.US_ASCII)
        );
    }

    public record EncryptedVerifier(String ciphertext, String nonce, int keyVersion) {
        @Override
        public String toString() {
            return "EncryptedVerifier[ciphertext=<redacted>, nonce=<redacted>, keyVersion=" + keyVersion + "]";
        }
    }
}
