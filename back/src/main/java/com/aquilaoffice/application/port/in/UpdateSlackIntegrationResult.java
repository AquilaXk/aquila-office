package com.aquilaoffice.application.port.in;

/**
 * Slack 수동 연동 수정 결과다.
 */
public record UpdateSlackIntegrationResult(
        Long id,
        Long workspaceId,
        String slackTeamId,
        String slackChannelId,
        String channelName,
        String maskedBotToken,
        String createdByGoogleSub,
        boolean active) {}
