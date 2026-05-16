package com.aquilaoffice.application.port.out;

/**
 * 내부 access/refresh token pair다.
 */
public record IssuedAuthTokens(String accessToken, String refreshToken) {

    public IssuedAuthTokens {
        accessToken = requireNotBlank(accessToken, "accessToken");
        refreshToken = requireNotBlank(refreshToken, "refreshToken");
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
