package com.aquilaoffice.global.security;

import com.aquilaoffice.global.config.properties.ExternalTokenEncryptionProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 외부 연동 token을 AES-GCM ciphertext로 저장하고, 기존 plaintext 저장값은 읽기 호환한다.
 */
@Component
public class AesGcmExternalTokenCipher implements ExternalTokenCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final String FORMAT_PREFIX = "enc:v1:";
    private static final int FORMAT_PARTS = 5;
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;

    private final SecretKeySpec keySpec;
    private final String keyId;
    private final SecureRandom secureRandom;

    @Autowired
    public AesGcmExternalTokenCipher(ExternalTokenEncryptionProperties properties) {
        this(properties, new SecureRandom());
    }

    AesGcmExternalTokenCipher(
            ExternalTokenEncryptionProperties properties,
            SecureRandom secureRandom) {
        this.keySpec = new SecretKeySpec(deriveKey(properties.key()), KEY_ALGORITHM);
        this.keyId = properties.keyId();
        this.secureRandom = secureRandom;
    }

    @Override
    public String encrypt(String plaintextToken) {
        String normalizedToken = requireNotBlank(plaintextToken, "plaintextToken");
        byte[] iv = new byte[IV_BYTES];
        secureRandom.nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD(keyId.getBytes(StandardCharsets.UTF_8));
            byte[] encryptedToken = cipher.doFinal(normalizedToken.getBytes(StandardCharsets.UTF_8));
            return FORMAT_PREFIX + keyId + ":" + encode(iv) + ":" + encode(encryptedToken);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("external token encryption failed", exception);
        }
    }

    @Override
    public String decrypt(String storedToken) {
        String normalizedToken = requireNotBlank(storedToken, "storedToken");
        if (!normalizedToken.startsWith(FORMAT_PREFIX)) {
            return normalizedToken;
        }

        String[] parts = normalizedToken.split(":", FORMAT_PARTS);
        if (parts.length != FORMAT_PARTS || !"enc".equals(parts[0]) || !"v1".equals(parts[1])) {
            throw new IllegalArgumentException("external token storage format is invalid");
        }
        String storedKeyId = parts[2];
        if (!keyId.equals(storedKeyId)) {
            throw new IllegalArgumentException("external token key id is not supported");
        }

        try {
            byte[] iv = decode(parts[3]);
            byte[] encryptedToken = decode(parts[4]);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD(storedKeyId.getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(encryptedToken), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("external token storage value is invalid", exception);
        }
    }

    private static byte[] deriveKey(String key) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is not available", exception);
        }
    }

    private static String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static byte[] decode(String value) {
        try {
            return Base64.getUrlDecoder().decode(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("external token storage format is invalid", exception);
        }
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
