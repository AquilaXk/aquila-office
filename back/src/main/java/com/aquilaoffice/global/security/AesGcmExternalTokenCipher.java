package com.aquilaoffice.global.security;

import com.aquilaoffice.global.config.properties.ExternalTokenEncryptionProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 외부 연동 token을 AES-GCM ciphertext로 저장하고 키 회전·컨텍스트 AAD·레거시 평문 읽기를 지원한다.
 */
@Component
public class AesGcmExternalTokenCipher implements ExternalTokenCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final String FORMAT_V1_PREFIX = "enc:v1:";
    private static final String FORMAT_V2_PREFIX = "enc:v2:";
    private static final int FORMAT_PARTS = 5;
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;

    private final SecretKeySpec primaryKeySpec;
    private final String primaryKeyId;
    private final Map<String, SecretKeySpec> keysById;
    private final SecureRandom secureRandom;

    @Autowired
    public AesGcmExternalTokenCipher(ExternalTokenEncryptionProperties properties) {
        this(properties, new SecureRandom());
    }

    AesGcmExternalTokenCipher(
            ExternalTokenEncryptionProperties properties,
            SecureRandom secureRandom) {
        this.primaryKeySpec = new SecretKeySpec(deriveKey(properties.key()), KEY_ALGORITHM);
        this.primaryKeyId = properties.keyId();

        Map<String, SecretKeySpec> keyMap = new LinkedHashMap<>();
        if (properties.previousKeys() != null) {
            for (Map.Entry<String, String> entry : properties.previousKeys().entrySet()) {
                keyMap.put(entry.getKey(), new SecretKeySpec(deriveKey(entry.getValue()), KEY_ALGORITHM));
            }
        }
        keyMap.put(this.primaryKeyId, this.primaryKeySpec);
        this.keysById = Collections.unmodifiableMap(keyMap);
        this.secureRandom = secureRandom;
    }

    @Override
    public String encrypt(String plaintextToken) {
        String normalizedToken = requireNotBlank(plaintextToken, "plaintextToken");
        byte[] iv = nextIv();
        byte[] aad = primaryKeyId.getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = encryptInternal(normalizedToken, iv, aad);
        return FORMAT_V1_PREFIX + primaryKeyId + ":" + encode(iv) + ":" + encode(encrypted);
    }

    @Override
    public String encryptWithContext(String plaintextToken, String context) {
        String normalizedToken = requireNotBlank(plaintextToken, "plaintextToken");
        String normalizedContext = requireNotBlank(context, "context");
        byte[] iv = nextIv();
        byte[] aad = (primaryKeyId + ":" + normalizedContext).getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = encryptInternal(normalizedToken, iv, aad);
        return FORMAT_V2_PREFIX + primaryKeyId + ":" + encode(iv) + ":" + encode(encrypted);
    }

    @Override
    public String decrypt(String storedToken) {
        String normalizedToken = requireNotBlank(storedToken, "storedToken");
        if (normalizedToken.startsWith(FORMAT_V2_PREFIX)) {
            throw new IllegalArgumentException("external token context is required for v2 ciphertext");
        }
        if (!normalizedToken.startsWith(FORMAT_V1_PREFIX)) {
            return normalizedToken;
        }
        return decryptVersioned(normalizedToken, "v1", null);
    }

    @Override
    public String decryptWithContext(String storedToken, String context) {
        String normalizedToken = requireNotBlank(storedToken, "storedToken");
        String normalizedContext = requireNotBlank(context, "context");
        if (normalizedToken.startsWith(FORMAT_V2_PREFIX)) {
            return decryptVersioned(normalizedToken, "v2", normalizedContext);
        }
        if (normalizedToken.startsWith(FORMAT_V1_PREFIX)) {
            return decryptVersioned(normalizedToken, "v1", null);
        }
        return normalizedToken;
    }

    private byte[] nextIv() {
        byte[] iv = new byte[IV_BYTES];
        secureRandom.nextBytes(iv);
        return iv;
    }

    private byte[] encryptInternal(String plaintext, byte[] iv, byte[] aad) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, primaryKeySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD(aad);
            return cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("external token encryption failed", exception);
        }
    }

    private String decryptVersioned(String normalizedToken, String expectedVersion, String context) {
        String[] parts = normalizedToken.split(":", FORMAT_PARTS);
        if (parts.length != FORMAT_PARTS || !"enc".equals(parts[0]) || !expectedVersion.equals(parts[1])) {
            throw new IllegalArgumentException("external token storage format is invalid");
        }
        String storedKeyId = parts[2];
        SecretKeySpec signingKey = keysById.get(storedKeyId);
        if (signingKey == null) {
            throw new IllegalArgumentException("external token key id is not supported");
        }

        byte[] iv = decode(parts[3]);
        if (iv.length != IV_BYTES) {
            throw new IllegalArgumentException("external token IV length is invalid");
        }
        byte[] encryptedToken = decode(parts[4]);
        byte[] aad = ("v2".equals(expectedVersion)
                ? storedKeyId + ":" + context
                : storedKeyId).getBytes(StandardCharsets.UTF_8);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, signingKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD(aad);
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
