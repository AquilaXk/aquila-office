package com.aquilaoffice.application.port.in;

/**
 * Refresh token 재발급 성공 후 client에 반환할 application 결과다.
 */
public record RefreshAuthTokenResult(String accessToken, String refreshToken) {

    public RefreshAuthTokenResult {
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
