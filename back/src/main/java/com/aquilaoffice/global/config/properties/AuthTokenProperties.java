package com.aquilaoffice.global.config.properties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 발급과 검증에 필요한 issuer, secret, TTL 설정이다.
 */
@ConfigurationProperties(prefix = "aquila.auth-token")
public record AuthTokenProperties(
        String issuer,
        String secret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl) {

    private static final int MINIMUM_HS256_SECRET_BYTES = 32;
    private static final String DEFAULT_ISSUER = "aquila-office";
    public static final String DEFAULT_DEVELOPMENT_SECRET =
            "local-development-jwt-secret-change-before-production-0123456789abcdef";
    private static final Duration DEFAULT_ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
    private static final Duration DEFAULT_REFRESH_TOKEN_TTL = Duration.ofDays(14);

    public AuthTokenProperties {
        issuer = defaultIfBlank(issuer, DEFAULT_ISSUER);
        secret = defaultIfBlank(secret, DEFAULT_DEVELOPMENT_SECRET);
        accessTokenTtl = defaultIfNull(accessTokenTtl, DEFAULT_ACCESS_TOKEN_TTL, "accessTokenTtl");
        refreshTokenTtl = defaultIfNull(refreshTokenTtl, DEFAULT_REFRESH_TOKEN_TTL, "refreshTokenTtl");
        int secretBytes = secret.getBytes(StandardCharsets.UTF_8).length;
        if (secretBytes < MINIMUM_HS256_SECRET_BYTES) {
            throw new IllegalArgumentException("secret must be at least 32 bytes for HS256");
        }
    }

    public boolean usesDefaultDevelopmentSecret() {
        return DEFAULT_DEVELOPMENT_SECRET.equals(secret);
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private static Duration defaultIfNull(Duration value, Duration defaultValue, String fieldName) {
        Duration resolvedValue = value == null ? defaultValue : value;
        if (resolvedValue.isZero() || resolvedValue.isNegative()) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return resolvedValue;
    }
}
