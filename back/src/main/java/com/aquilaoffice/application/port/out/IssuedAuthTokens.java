package com.aquilaoffice.application.port.out;

import java.time.Instant;

/**
 * 내부 access/refresh token pair다.
 */
public record IssuedAuthTokens(
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiresAt,
        Instant refreshTokenExpiresAt,
        String refreshSessionId) {

    public IssuedAuthTokens {
        accessToken = requireNotBlank(accessToken, "accessToken");
        refreshToken = requireNotBlank(refreshToken, "refreshToken");
        accessTokenExpiresAt = requireNotNull(accessTokenExpiresAt, "accessTokenExpiresAt");
        refreshTokenExpiresAt = requireNotNull(refreshTokenExpiresAt, "refreshTokenExpiresAt");
        refreshSessionId = requireNotBlank(refreshSessionId, "refreshSessionId");
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static Instant requireNotNull(Instant value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }
}
