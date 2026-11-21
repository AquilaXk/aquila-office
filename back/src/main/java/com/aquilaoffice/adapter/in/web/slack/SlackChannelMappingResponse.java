package com.aquilaoffice.adapter.in.web.slack;

import com.aquilaoffice.application.port.in.CreateSlackChannelMappingResult;
import com.aquilaoffice.application.port.in.ListSlackChannelMappingsResult;

/**
 * Slack channel mapping HTTP 응답 모델이다.
 */
public record SlackChannelMappingResponse(
        Long mappingId,
        Long workspaceId,
        String teamId,
        String channelId,
        String channelName,
        String maskedBotToken,
        String createdByGoogleSub,
        boolean active) {

    public static SlackChannelMappingResponse from(CreateSlackChannelMappingResult result) {
        return new SlackChannelMappingResponse(
                result.mappingId(),
                result.workspaceId(),
                result.teamId(),
                result.channelId(),
                result.channelName(),
                result.maskedBotToken(),
                result.createdByGoogleSub(),
                result.active());
    }

    public static SlackChannelMappingResponse from(ListSlackChannelMappingsResult result) {
        return new SlackChannelMappingResponse(
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
