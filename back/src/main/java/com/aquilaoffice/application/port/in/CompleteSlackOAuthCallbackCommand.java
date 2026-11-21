package com.aquilaoffice.application.port.in;

/**
 * Slack OAuth callback 처리 command다.
 */
public record CompleteSlackOAuthCallbackCommand(
        String code,
        String state,
        String redirectUri,
        String requesterGoogleSub) {

    public CompleteSlackOAuthCallbackCommand {
        code = requireNotBlank(code, "code");
        state = requireNotBlank(state, "state");
        redirectUri = requireNotBlank(redirectUri, "redirectUri");
        requesterGoogleSub = requireNotBlank(requesterGoogleSub, "requesterGoogleSub");
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
