package com.aquilaoffice.application.port.in;

/**
 * Slack channel mapping 생성 결과다.
 */
public record CreateSlackChannelMappingResult(
        Long mappingId,
        Long workspaceId,
        String teamId,
        String channelId,
        String channelName,
        String createdByGoogleSub,
        boolean active) {}
