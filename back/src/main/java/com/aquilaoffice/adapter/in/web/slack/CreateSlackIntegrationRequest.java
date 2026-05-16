package com.aquilaoffice.adapter.in.web.slack;

import com.aquilaoffice.application.port.in.CreateSlackChannelMappingCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * Slack 수동 연동 생성 HTTP 요청이다.
 */
public record CreateSlackIntegrationRequest(
        @NotBlank String slackTeamId,
        @NotBlank String slackChannelId,
        String channelName,
        @NotBlank String botToken) {

    public CreateSlackChannelMappingCommand toCommand(Long workspaceId, String requesterGoogleSub) {
        return new CreateSlackChannelMappingCommand(
                workspaceId,
                requesterGoogleSub,
                slackTeamId,
                slackChannelId,
                channelName,
                botToken);
    }
}
