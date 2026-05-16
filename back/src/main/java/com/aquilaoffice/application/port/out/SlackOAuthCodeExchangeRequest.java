package com.aquilaoffice.application.port.out;

/**
 * Slack OAuth token exchange 요청 값이다.
 */
public record SlackOAuthCodeExchangeRequest(
        String code,
        String redirectUri) {

    public SlackOAuthCodeExchangeRequest {
        code = requireNotBlank(code, "code");
        redirectUri = requireNotBlank(redirectUri, "redirectUri");
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
