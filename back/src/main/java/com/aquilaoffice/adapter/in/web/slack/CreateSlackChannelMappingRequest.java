package com.aquilaoffice.adapter.in.web.slack;

import com.aquilaoffice.application.port.in.CreateSlackChannelMappingCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * Slack channel mapping 생성 HTTP 요청이다.
 */
public record CreateSlackChannelMappingRequest(
        @NotBlank String teamId,
        @NotBlank String channelId,
        String channelName) {

    public CreateSlackChannelMappingCommand toCommand(Long workspaceId, String requesterGoogleSub) {
        return new CreateSlackChannelMappingCommand(
                workspaceId,
                requesterGoogleSub,
                teamId,
                channelId,
                channelName);
    }
}
