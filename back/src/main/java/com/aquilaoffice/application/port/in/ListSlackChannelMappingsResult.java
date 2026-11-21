package com.aquilaoffice.application.port.in;

/**
 * Slack channel mapping 목록 항목 결과다.
 */
public record ListSlackChannelMappingsResult(
        Long mappingId,
        Long workspaceId,
        String teamId,
        String channelId,
        String channelName,
        String maskedBotToken,
        String createdByGoogleSub,
        boolean active) {}
