package com.aquilaoffice.application.port.out;

/**
 * Slack OAuth token exchange 성공 결과다.
 */
public record SlackOAuthCodeExchangeResult(
        String teamId,
        String channelId,
        String channelName,
        String botToken) {

    public SlackOAuthCodeExchangeResult {
        teamId = requireNotBlank(teamId, "teamId");
        channelId = requireNotBlank(channelId, "channelId");
        channelName = normalizeOptional(channelName);
        botToken = requireNotBlank(botToken, "botToken");
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
