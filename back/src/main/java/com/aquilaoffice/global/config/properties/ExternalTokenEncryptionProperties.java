package com.aquilaoffice.global.config.properties;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 외부 연동 token 저장 암호화에 사용하는 key와 저장 format 식별자 설정이다.
 */
@ConfigurationProperties(prefix = "aquila.external-token")
public record ExternalTokenEncryptionProperties(
        String key,
        String keyId) {

    private static final int MINIMUM_KEY_BYTES = 32;
    private static final String DEFAULT_KEY =
            "local-development-external-token-key-change-before-production-0123456789abcdef";
    private static final String DEFAULT_KEY_ID = "local";
    private static final Pattern KEY_ID_PATTERN = Pattern.compile("[A-Za-z0-9_.-]{1,64}");

    public ExternalTokenEncryptionProperties {
        key = defaultIfBlank(key, DEFAULT_KEY);
        keyId = defaultIfBlank(keyId, DEFAULT_KEY_ID);
        int keyBytes = key.getBytes(StandardCharsets.UTF_8).length;
        if (keyBytes < MINIMUM_KEY_BYTES) {
            throw new IllegalArgumentException("key must be at least 32 bytes for external token encryption");
        }
        if (!KEY_ID_PATTERN.matcher(keyId).matches()) {
            throw new IllegalArgumentException("keyId must contain 1-64 safe identifier characters");
        }
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }
}
