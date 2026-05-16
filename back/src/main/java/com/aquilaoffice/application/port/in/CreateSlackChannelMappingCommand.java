package com.aquilaoffice.application.port.in;

/**
 * Slack channel mapping 생성 command다.
 */
public record CreateSlackChannelMappingCommand(
        Long workspaceId,
        String requesterGoogleSub,
        String teamId,
        String channelId,
        String channelName) {

    public CreateSlackChannelMappingCommand {
        requesterGoogleSub = normalizeRequired(requesterGoogleSub, "requesterGoogleSub");
        teamId = normalizeRequired(teamId, "teamId");
        channelId = normalizeRequired(channelId, "channelId");
        channelName = normalizeOptional(channelName);
    }

    private static String normalizeRequired(String value, String fieldName) {
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
