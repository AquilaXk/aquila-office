package com.aquilaoffice.adapter.in.web.slack;

import com.aquilaoffice.application.port.in.UpdateSlackIntegrationCommand;
import jakarta.validation.constraints.Size;

/**
 * Slack 수동 연동 부분 수정 HTTP 요청이다.
 */
public record UpdateSlackIntegrationRequest(
        @Size(max = 120) String slackTeamId,
        @Size(max = 120) String slackChannelId,
        @Size(max = 120) String channelName,
        String botToken) {

    UpdateSlackIntegrationCommand toCommand(Long workspaceId, Long integrationId, String requesterGoogleSub) {
        return new UpdateSlackIntegrationCommand(
                workspaceId,
                requesterGoogleSub,
                integrationId,
                slackTeamId,
                slackChannelId,
                channelName,
                botToken);
    }
}
