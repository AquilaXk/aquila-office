package com.aquilaoffice.adapter.in.web.slack;

import com.aquilaoffice.application.port.in.CreateSlackChannelMappingResult;
import com.aquilaoffice.application.port.in.ListSlackChannelMappingsResult;

/**
 * Slack 수동 연동 HTTP 응답 모델이다.
 */
public record SlackIntegrationResponse(
        Long id,
        Long workspaceId,
        String slackTeamId,
        String slackChannelId,
        String channelName,
        String maskedBotToken,
        String createdByGoogleSub,
        boolean active) {

    public static SlackIntegrationResponse from(CreateSlackChannelMappingResult result) {
        return new SlackIntegrationResponse(
                result.mappingId(),
                result.workspaceId(),
                result.teamId(),
                result.channelId(),
                result.channelName(),
                result.maskedBotToken(),
                result.createdByGoogleSub(),
                result.active());
    }

    public static SlackIntegrationResponse from(ListSlackChannelMappingsResult result) {
        return new SlackIntegrationResponse(
                result.mappingId(),
                result.workspaceId(),
                result.teamId(),
                result.channelId(),
                result.channelName(),
                result.maskedBotToken(),
                result.createdByGoogleSub(),
                result.active());
    }
}
