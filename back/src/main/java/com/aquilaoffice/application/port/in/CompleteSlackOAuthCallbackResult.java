package com.aquilaoffice.application.port.in;

/**
 * Slack OAuth callback 처리 결과다.
 */
public record CompleteSlackOAuthCallbackResult(
        Long id,
        Long workspaceId,
        String slackTeamId,
        String slackChannelId,
        String channelName,
        String maskedBotToken,
        String createdByGoogleSub,
        boolean active) {}
