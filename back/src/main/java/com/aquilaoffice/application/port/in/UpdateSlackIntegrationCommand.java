package com.aquilaoffice.application.port.in;

/**
 * Slack 수동 연동 수정 command다.
 */
public record UpdateSlackIntegrationCommand(
        Long workspaceId,
        String requesterGoogleSub,
        Long integrationId,
        String slackTeamId,
        String slackChannelId,
        String channelName,
        String botToken) {

    public UpdateSlackIntegrationCommand {
        workspaceId = requirePositive(workspaceId, "workspaceId");
        requesterGoogleSub = requireNotBlank(requesterGoogleSub, "requesterGoogleSub");
        integrationId = requirePositive(integrationId, "integrationId");
        slackTeamId = normalizeOptional(slackTeamId);
        slackChannelId = normalizeOptional(slackChannelId);
        channelName = normalizeOptional(channelName);
        botToken = normalizeOptional(botToken);
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
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
