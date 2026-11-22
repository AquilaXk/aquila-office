package com.aquilaoffice.global.config.properties;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

/**
 * 외부 연동 token 저장 암호화에 사용하는 key와 저장 format 식별자 설정이다.
 */
@ConfigurationProperties(prefix = "aquila.external-token")
public record ExternalTokenEncryptionProperties(
        String key,
        String keyId,
        Map<String, String> previousKeys) {

    private static final int MINIMUM_KEY_BYTES = 32;
    public static final String DEFAULT_DEVELOPMENT_KEY =
            "local-development-external-token-key-change-before-production-0123456789abcdef";
    public static final String DEFAULT_DEVELOPMENT_KEY_ID = "local";
    private static final Pattern KEY_ID_PATTERN = Pattern.compile("[A-Za-z0-9_.-]{1,64}");

    public ExternalTokenEncryptionProperties(String key, String keyId) {
        this(key, keyId, Map.of());
    }

    @ConstructorBinding
    public ExternalTokenEncryptionProperties {
        key = defaultIfBlank(key, DEFAULT_DEVELOPMENT_KEY);
        keyId = defaultIfBlank(keyId, DEFAULT_DEVELOPMENT_KEY_ID);
        validateKey(key, "key");
        validateKeyId(keyId, "keyId");

        Map<String, String> normalizedPrevious = new LinkedHashMap<>();
        if (previousKeys != null) {
            for (Map.Entry<String, String> entry : previousKeys.entrySet()) {
                String prevId = entry.getKey() == null ? null : entry.getKey().trim();
                String prevKey = entry.getValue() == null ? null : entry.getValue().trim();
                validateKeyId(prevId, "previousKeys keyId");
                validateKey(prevKey, "previousKeys[" + prevId + "]");
                normalizedPrevious.put(prevId, prevKey);
            }
        }
        previousKeys = Collections.unmodifiableMap(normalizedPrevious);
    }

    private static void validateKey(String candidate, String label) {
        if (candidate == null || candidate.getBytes(StandardCharsets.UTF_8).length < MINIMUM_KEY_BYTES) {
            throw new IllegalArgumentException(label + " must be at least 32 bytes for external token encryption");
        }
    }

    private static void validateKeyId(String candidate, String label) {
        if (candidate == null || !KEY_ID_PATTERN.matcher(candidate).matches()) {
            throw new IllegalArgumentException(label + " must contain 1-64 safe identifier characters");
        }
    }

    public boolean usesDefaultDevelopmentKey() {
        return DEFAULT_DEVELOPMENT_KEY.equals(key);
    }

    public boolean usesDefaultDevelopmentKeyId() {
        return DEFAULT_DEVELOPMENT_KEY_ID.equals(keyId);
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }
}
